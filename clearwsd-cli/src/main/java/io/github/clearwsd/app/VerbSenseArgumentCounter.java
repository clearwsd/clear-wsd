/*
 * Copyright (C) 2017  James Gung
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.clearwsd.app;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import io.github.clearwsd.WordSenseAnnotator;
import io.github.clearwsd.type.DepNode;
import io.github.clearwsd.type.DepTree;
import io.github.clearwsd.type.FeatureType;
import io.github.clearwsd.WordSenseAnnotator;
import io.github.clearwsd.WordSenseClassifier;
import io.github.clearwsd.corpus.CorpusReader;
import io.github.clearwsd.corpus.TextCorpusReader;
import io.github.clearwsd.corpus.semlink.VerbNetReader;
import io.github.clearwsd.feature.annotator.Annotator;
import io.github.clearwsd.parser.StanfordDependencyParser;
import io.github.clearwsd.verbnet.DefaultPredicateAnnotator;
import edu.stanford.nlp.util.Comparators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for counting arguments of verbs by sense. Can apply a trained classifier, or read from an existing annotated corpus.
 * Saves to a file-backed DB via MapDB (https://github.com/jankotek/mapdb).
 *
 * @author jamesgung
 */
@Slf4j
public class VerbSenseArgumentCounter {

    @Data
    @AllArgsConstructor
    private static class Argument implements Serializable {
        private static final long serialVersionUID = 2148073117043088204L;
        private String predicate;
        private String sense;
        private String relation;
        private String argument;

        @Override
        public String toString() {
            return String.format("%s\t%s\t%s\t%s", predicate, sense, relation, argument);
        }
    }

    @Getter
    private static class ArgumentGroup {
        String key;
        List<Map.Entry<Argument, Long>> entries;
        long total;

        ArgumentGroup(String key, List<Map.Entry<Argument, Long>> entries) {
            this.key = key;
            this.entries = entries;
            total = entries.stream().mapToLong(Map.Entry::getValue).sum();
        }
    }

    @Getter
    private enum ArgField {
        PREDICATE(Comparator.comparing(a -> a.getKey().predicate), a -> a.getKey().predicate),
        SENSE(Comparator.comparing(a -> a.getKey().sense), a -> a.getKey().sense),
        RELATION(Comparator.comparing(a -> a.getKey().relation), a -> a.getKey().relation),
        ARGUMENT(Comparator.comparing(a -> a.getKey().argument), a -> a.getKey().argument),
        COUNT((a1, a2) -> Long.compare(a2.getValue(), a1.getValue()), a -> Long.toString(a.getValue()));

        Comparator<Map.Entry<Argument, Long>> comparator;
        Function<Map.Entry<Argument, Long>, String> toString;

        ArgField(Comparator<Map.Entry<Argument, Long>> comparator, Function<Map.Entry<Argument, Long>, String> toString) {
            this.comparator = comparator;
            this.toString = toString;
        }
    }

    private static final String COUNTS = "counts";
    private static final String COMPLETE = "complete";
    private static final String PARSED = "parsed";

    @Parameter(names = {"-corpus", "-c"}, description = "Path to corpus directory or file for argument counting", required = true)
    private String corpusPath;
    @Parameter(names = "-ext", description = "Extension of parsed files in corpus to be processed")
    private String corpusExt = ".dep.gz";
    @Parameter(names = "-raw", description = "Extension of raw files in corpus to be parsed")
    private String rawExt = ".txt";
    @Parameter(names = {"-model", "-m"}, description = "Path to word sense classifier model file")
    private String modelPath;
    @Parameter(names = "-db", description = "Path to MapDB DB file to persist or restore counts", required = true)
    private String dbPath;
    @Parameter(names = {"-outputDir", "-o", "-out"}, description = "Path to output directory")
    private String outputPath;
    @Parameter(names = "-outputExt", description = "Output file extension")
    private String outputExt = ".txt";
    @Parameter(names = "-relations", description = "Relations to be included in counts")
    private Set<String> relations = Sets.newHashSet("dobj");
    @Parameter(names = "--update", description = "Update existing DB")
    private boolean update = true;
    @Parameter(names = "--overwrite", description = "Overwrite existing DB")
    private boolean overwrite = false;
    @Parameter(names = "--parseOnly", description = "Only parse, don't count or apply annotator")
    private boolean parseOnly = false;
    @Parameter(names = "--reparse", description = "Parse even if there is an existing parsed file")
    private boolean reparse = false;

    @Parameter(names = "-limit", description = "Maximum number of entries to return in output", order = 900)
    private int limit = 10000000;
    @Parameter(names = "-threshold", description = "Minimum count to be returned in output", order = 900)
    private int threshold = 5;
    @Parameter(names = "-sort", description = "List of fields to sort by in output", order = 901)
    private List<ArgField> sortFields = Collections.singletonList(ArgField.COUNT);
    @Parameter(names = "-print", description = "List of fields to print (in order) in output", order = 902)
    private List<ArgField> printFields = Arrays.asList(ArgField.ARGUMENT, ArgField.COUNT);
    @Parameter(names = "-group", description = "Field to group by in output", order = 903)
    private List<ArgField> groupBy = Arrays.asList(ArgField.PREDICATE, ArgField.SENSE);
    @Parameter(names = {"-separator", "-sep"}, description = "Separator character", order = 904)
    private String sep = ",";

    private CorpusReader<DepTree> corpusReader = new VerbNetReader.VerbNetCoNllDepReader();
    private Annotator<DepTree> senseAnnotator;
    private DB db;
    private HTreeMap<Argument, Long> countMap;
    private Set<String> parsed;
    private Set<String> processed;

    private VerbSenseArgumentCounter(String... args) {
        JCommander cmd = new JCommander(this);
        cmd.setProgramName(this.getClass().getSimpleName());
        try {
            if (args.length == 0) {
                cmd.usage();
                System.exit(0);
            }
            cmd.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            cmd.usage();
            System.exit(1);
        }
        initializeDb();
        initializeAnnotator();
        validateDir();
    }

    private void validateDir() {
        if (outputPath == null) {
            return;
        }
        File outputFile = new File(outputPath);
        if (!outputFile.exists() && !outputFile.mkdirs()) {
            throw new RuntimeException("Unable to create save directory");
        }
        if (outputFile.isFile()) {
            throw new IllegalArgumentException("Output path provided is an existing file (" + outputPath + "), "
                    + "please provide a directory path.");
        }
    }

    private void initializeDb() {
        if (!update && !overwrite && new File(dbPath).exists()) {
            throw new RuntimeException("DB already exists at specified location. Please specify '--update' or '--overwrite' "
                    + "to loading existing DB or overwrite. Otherwise, change the path name.");
        }
        if (overwrite && new File(dbPath).exists()) {
            boolean deleted = new File(dbPath).delete();
            if (!deleted) {
                throw new RuntimeException("Unable to remove old DB");
            }
        }
        db = DBMaker.fileDB(dbPath)
                .fileMmapEnable()
                .closeOnJvmShutdown()
                .make();
        if (overwrite) {
            //noinspection unchecked
            countMap = db.<Argument, Long>hashMap(COUNTS, Serializer.JAVA, Serializer.LONG).create();
            parsed = db.hashSet(PARSED).serializer(Serializer.STRING).create();
            processed = db.hashSet(COMPLETE).serializer(Serializer.STRING).create();
        } else {
            //noinspection unchecked
            countMap = db.<Argument, Long>hashMap(COUNTS, Serializer.JAVA, Serializer.LONG).createOrOpen();
            parsed = db.hashSet(PARSED).serializer(Serializer.STRING).createOrOpen();
            processed = db.hashSet(COMPLETE).serializer(Serializer.STRING).createOrOpen();
        }
    }

    private void initializeAnnotator() {
        if (modelPath != null && !parseOnly) {
            Stopwatch sw = Stopwatch.createStarted();
            log.debug("Loading sense annotator at {}...", modelPath);
            try {
                WordSenseClassifier classifier = WordSenseClassifier.load(new File(modelPath).toURI().toURL());
                senseAnnotator = new WordSenseAnnotator(classifier, new DefaultPredicateAnnotator(
                        classifier.predicateDictionary()));
            } catch (Exception e) {
                throw new RuntimeException("Unable to load word sense classifier model: " + e.getMessage(), e);
            }
            log.debug("Loaded sense annotator in {}", sw);
        }
    }

    private List<File> getCorpusFiles(String ext) {
        List<File> results = new ArrayList<>();
        try {
            File corpusFile = new File(corpusPath);
            if (corpusFile.isDirectory()) {
                Files.walk(Paths.get(corpusPath), 999)
                        .filter(path -> path.toString().endsWith(ext))
                        .map(Path::toFile)
                        .forEach(results::add);
            } else {
                results.add(corpusFile);
            }
        } catch (IOException e) {
            log.warn("Error reading corpus files at {}", corpusPath, e);
        }
        return results;
    }

    private void run() {
        List<File> toParse = getCorpusFiles(rawExt).stream()
                .filter(f -> !processed.contains(f.getPath() + corpusExt))
                .filter(f -> reparse || !(new File(f.getPath() + corpusExt).exists()))
                .collect(Collectors.toList());
        if (toParse.size() > 0) {
            log.debug("Found {} files ending in {} at {}", toParse.size(), rawExt, corpusPath);
            TextCorpusReader reader = new TextCorpusReader(new StanfordDependencyParser());
            toParse.parallelStream().forEach(
                    file -> {
                        if (!parsed.contains(file.getPath())) {
                            try (FileInputStream inputStream = new FileInputStream(file);
                                 OutputStream outputStream = new GZIPOutputStream(
                                         new FileOutputStream(file.getPath() + corpusExt))) {
                                // read instances
                                log.debug("Parsing file {} and saving trees to {}", file, file.getPath() + corpusExt);
                                reader.parseAndWrite(inputStream, outputStream, 1000);
                                parsed.add(file.getPath());
                            } catch (IOException e) {
                                log.warn("Unable to process file {}", file, e);
                            }
                        }
                    }
            );
        }
        if (!parseOnly) {
            for (File file : getCorpusFiles(corpusExt)) {
                if (processed.contains(file.getPath())) { // skip already-processed files
                    continue;
                }
                log.debug("Reading file: {}", file.getAbsolutePath());
                Stopwatch sw = Stopwatch.createStarted();
                int index = 0;
                Iterator<DepTree> iterator = readTrees(file);
                while (iterator.hasNext()) {
                    process(iterator.next());
                    if (++index % 10000 == 0) {
                        log.debug("Processing {} trees/second ({} total).", index / sw.elapsed(TimeUnit.SECONDS), index);
                    }
                }
                processed.add(file.getPath());
            }
            outputEntries();
        }
        db.close();
    }

    private Iterator<DepTree> readTrees(File file) {
        try {
            InputStream is = new GZIPInputStream(new FileInputStream(file));
            return corpusReader.instanceIterator(is);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file at " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Update counts for a single dependency tree.
     *
     * @param tree dependency tree
     */
    private void process(DepTree tree) {
        if (senseAnnotator != null) {
            senseAnnotator.annotate(tree);
        }
        for (DepNode depNode : tree) {
            String sense = depNode.feature(FeatureType.Sense);
            if (null == sense || sense.isEmpty()) {
                continue;
            }
            String lemma = depNode.feature(FeatureType.Predicate);
            //noinspection SuspiciousMethodCalls
            depNode.children().stream()
                    .filter(child -> relations.contains(child.feature(FeatureType.Dep)))
                    .forEach(child -> {
                        Argument key = new Argument(lemma, sense,
                                child.feature(FeatureType.Dep),
                                child.feature(FeatureType.Lemma));
                        Long result = countMap.getOrDefault(key, 0L);
                        countMap.put(key, result + 1);
                    });
        }
    }

    private String formatted(Map.Entry<Argument, Long> entry, List<ArgField> fields) {
        return String.join(sep, fields.stream()
                .map(f -> f.getToString().apply(entry))
                .collect(Collectors.toList()));
    }

    private List<ArgumentGroup> group(List<ArgField> fields, List<Map.Entry<Argument, Long>> counts) {
        ListMultimap<String, Map.Entry<Argument, Long>> index = Multimaps.index(counts, entry ->
                fields.stream().map(f -> f.getToString().apply(entry)).collect(Collectors.joining(sep)));
        return index.asMap().entrySet().stream()
                .map(e -> new ArgumentGroup(e.getKey(), (List<Map.Entry<Argument, Long>>) e.getValue()))
                .collect(Collectors.toList());
    }

    private void outputEntries() {
        List<Map.Entry<Argument, Long>> counts = countMap.getEntries().stream()
                .sorted((a1, a2) -> Long.compare(a2.getValue(), a1.getValue()))
                .filter(s -> s.getValue() > threshold)
                .limit(limit)
                .collect(Collectors.toList());
        // group entries by provided field
        List<ArgumentGroup> groups = groupBy.size() == 0
                ? Collections.singletonList(new ArgumentGroup(COUNTS, counts)) : group(groupBy, counts);
        groups.sort(Comparator.comparingLong(ArgumentGroup::getTotal).reversed());

        if (outputPath != null) {
            log.debug("Saving results to {}", outputPath);
        }
        for (ArgumentGroup argumentGroup : groups) {
            // sort grouped entries by sort fields
            Comparator<Map.Entry<Argument, Long>> comparator = Comparators.chain(sortFields.stream()
                    .map(ArgField::getComparator)
                    .collect(Collectors.toList()));
            List<Map.Entry<Argument, Long>> sorted = argumentGroup.getEntries().stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
            // print entries with provided formatter
            if (outputPath != null) {
                try (PrintWriter writer = new PrintWriter(new File(outputPath, argumentGroup.getKey().replaceAll(
                        "[^a-zA-Z0-9.\\-]", "_") + outputExt))) {
                    for (Map.Entry<Argument, Long> entry : sorted) {
                        writer.println(formatted(entry, printFields));
                    }
                } catch (FileNotFoundException e) {
                    log.warn("Unable to write results to file", e);
                }
            } else {
                System.out.println(argumentGroup.getKey());
                for (Map.Entry<Argument, Long> entry : sorted) {
                    System.out.println(formatted(entry, printFields));
                }
                System.out.println();
            }
        }
    }

    public static void main(String... args) {
        new VerbSenseArgumentCounter(args).run();
    }

}

package edu.colorado.clear.wsd.corpus.semlink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.colorado.clear.parser.NlpParser;
import edu.colorado.clear.parser.NlpTokenizer;
import edu.colorado.clear.type.DepNode;
import edu.colorado.clear.type.DepTree;
import edu.colorado.clear.type.NlpFocus;
import edu.colorado.clear.wsd.corpus.CorpusReader;
import edu.colorado.clear.wsd.corpus.semlink.VerbNetReader.VerbNetInstanceParser;
import edu.colorado.clear.wsd.parser.WhitespaceTokenizer;
import edu.colorado.clear.wsd.type.DefaultNlpFocus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static edu.colorado.clear.type.FeatureType.Gold;
import static edu.colorado.clear.type.FeatureType.Lemma;
import static edu.colorado.clear.type.FeatureType.Metadata;
import static edu.colorado.clear.type.FeatureType.Predicate;
import static edu.colorado.clear.type.FeatureType.Text;

/**
 * Corpus reader that reads and parses SemLink-style annotations.
 *
 * @author jamesgung
 */
@Slf4j
public class ParsingSemlinkReader implements CorpusReader<NlpFocus<DepNode, DepTree>> {

    private NlpParser dependencyParser;
    private NlpTokenizer tokenizer;

    @Setter
    private boolean writeSemlink = false;

    public ParsingSemlinkReader(NlpParser dependencyParser, NlpTokenizer tokenizer) {
        this.dependencyParser = dependencyParser;
        this.tokenizer = tokenizer;
    }

    /**
     * Initialize a {@link ParsingSemlinkReader} with a {@link WhitespaceTokenizer}. Assumes instances are pre-tokenized.
     *
     * @param dependencyParser dependency parser
     */
    public ParsingSemlinkReader(NlpParser dependencyParser) {
        this(dependencyParser, new WhitespaceTokenizer());
    }

    @Override
    public List<NlpFocus<DepNode, DepTree>> readInstances(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            VerbNetInstanceParser parser = new VerbNetInstanceParser();
            List<NlpFocus<DepNode, DepTree>> results = new ArrayList<>();
            int index = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                VerbNetReader.VerbNetInstance instance = parser.parse(line);
                DepTree depTree = dependencyParser.parse(tokenizer.tokenize(instance.originalText()));

                DepNode focus = depTree.get(instance.token());
                if (!instance.lemma().equalsIgnoreCase(focus.feature(Lemma))) {
                    log.warn("Lemma mismatch ({} vs. {}) between annotation and parser output for instance: {}",
                            instance.lemma(), focus.feature(Lemma), line);
                }
                focus.addFeature(Gold, instance.label());
                focus.addFeature(Predicate, instance.lemma());

                NlpFocus<DepNode, DepTree> focusInstance = new DefaultNlpFocus<>(index++, focus, depTree);
                focusInstance.addFeature(Gold, instance.label());
                focusInstance.addFeature(Metadata, line);
                if (index % 1000 == 0) {
                    log.debug("VerbNet parsing progress: {} instances", index);
                }
                results.add(focusInstance);
            }
            log.debug("Read {} instances with {}.", results.size(), this.getClass().getSimpleName());
            return results;
        } catch (IOException e) {
            throw new RuntimeException("Error reading annotations.", e);
        }
    }

    @Override
    public void writeInstances(List<NlpFocus<DepNode, DepTree>> instances, OutputStream outputStream) {
        if (!writeSemlink) {
            new VerbNetReader().writeInstances(instances, outputStream);
        }
        try (PrintWriter writer = new PrintWriter(outputStream)) {
            for (NlpFocus<DepNode, DepTree> instance : instances) {
                String result = instance.feature(Metadata);
                if (result == null) {
                    result = String.format("%d %d %d %s %s\t%s",
                            instance.index(),
                            instance.index(),
                            instance.focus().index(),
                            instance.focus().feature(Predicate),
                            instance.focus().feature(Gold),
                            instance.sequence().tokens().stream()
                                    .map(t -> (String) t.feature(Text))
                                    .collect(Collectors.joining(" ")));
                }
                writer.println(result);
            }
        }
    }

    public static List<NlpFocus<DepNode, DepTree>> getFocusInstances(List<DepTree> dependencyTrees) {
        List<NlpFocus<DepNode, DepTree>> instances = new ArrayList<>();
        for (DepTree dependencyTree : dependencyTrees) {
            for (DepNode depNode : dependencyTree) {
                if (depNode.feature(Predicate) != null) {
                    instances.add(new DefaultNlpFocus<>(instances.size(), depNode, dependencyTree));
                }
            }
        }
        return instances;
    }

}

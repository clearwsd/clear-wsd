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

import com.google.common.collect.Multimap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.util.Map;

import io.github.clearwsd.utils.EmbeddingIoUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for converting word embeddings to binary sparse vectors (based on
 * "Revisiting Embedding Features for Simple Semi-supervised Learning" (Guo et al. 2014).
 *
 * @author jamesgung
 */
@Slf4j
public class EmbeddingBinarizer {

    @Parameter(names = {"-inputPath", "-i"}, description = "Input path to vector file", required = true)
    private String inputPath;
    @Parameter(names = {"-outputPath", "-o"}, description = "Output path")
    private String outputPath;
    @Parameter(names = "-limit", description = "Maximum number of vectors to read")
    private int limit = -1;

    private EmbeddingBinarizer(String... args) {
        JCommander cmd = new JCommander(this);
        cmd.setProgramName(this.getClass().getSimpleName());
        try {
            cmd.parse(args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            cmd.usage();
            System.exit(1);
        }
    }

    private void run() {
        Map<String, float[]> vectors = EmbeddingIoUtils.readVectors(inputPath, limit, true);
        log.info("Binarizing {} vectors...", vectors.size());
        Multimap<String, Integer> binarized = EmbeddingIoUtils.binarize(vectors);
        if (outputPath == null) {
            outputPath = new File(inputPath + ".binarized.txt").getAbsolutePath();
        }
        log.info("Writing binarized vectors to {}", outputPath);
        EmbeddingIoUtils.printMultimapTsv(binarized, new File(outputPath));
    }

    public static void main(String... args) {
        new EmbeddingBinarizer(args).run();
    }

}

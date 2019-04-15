/*
 * Copyright 2017 James Gung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.clearwsd.corpus;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import io.github.clearwsd.type.NlpInstance;
import java.util.Set;

/**
 * Corpus reader/writer.
 *
 * @param <T> instance type
 * @author jamesgung
 */
public interface CorpusReader<T extends NlpInstance> {

    /**
     * Read all instances from a given {@link InputStream}.
     *
     * @param inputStream instance input stream
     * @return list of instances
     */
    List<T> readInstances(InputStream inputStream);

    /**
     * Read all instances from a given {@link InputStream}.
     *
     * @param inputStream instance input stream
     * @param filter optional set of types to filter by
     * @return list of instances
     */
    default List<T> readInstances(InputStream inputStream, Set<String> filter) {
        return readInstances(inputStream);
    }

    /**
     * Write a list of instances to a given output stream.
     *
     * @param instances    list of instances
     * @param outputStream target output
     */
    void writeInstances(List<T> instances, OutputStream outputStream);

    /**
     * Create an instance iterator, useful when the input corpus is too large to fit into memory.
     *
     * @param inputStream corpus input stream
     * @return iterator over instances
     */
    default Iterator<T> instanceIterator(InputStream inputStream) {
        List<T> instances = readInstances(inputStream);
        return instances.iterator();
    }

}

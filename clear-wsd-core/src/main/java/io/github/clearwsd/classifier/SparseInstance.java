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

package io.github.clearwsd.classifier;

/**
 * Learning instance represented as a sparse vector with an associated target index and unique identifier.
 *
 * @author jamesgung
 */
public interface SparseInstance extends SparseVector {

    /**
     * Unique identifier for this instance.
     */
    int id();

    /**
     * Target index associated with this instance.
     */
    int target();

}

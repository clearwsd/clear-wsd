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

package io.github.clearwsd.feature.context;

import org.junit.Test;

import java.util.List;

import io.github.clearwsd.type.DepNode;
import io.github.clearwsd.type.DepTree;
import io.github.clearwsd.type.NlpFocus;
import io.github.clearwsd.feature.TestInstanceBuilder;

import static junit.framework.TestCase.assertEquals;

/**
 * @author jamesgung
 */
public class OffsetContextFactoryTest {

    private NlpFocus<DepNode, DepTree> getTestInstance() {
        return new TestInstanceBuilder("0 1 2 3 4 5 6", 3).build();
    }

    @Test
    public void testContextFocus() {
        OffsetContextFactory<DepNode, DepTree> factory = new OffsetContextFactory<>(0);
        List<NlpContext<DepNode>> contexts = factory.apply(getTestInstance());
        assertEquals(1, contexts.size());
        assertEquals(1, contexts.get(0).tokens().size());
        assertEquals(3, contexts.get(0).tokens().get(0).index());
        assertEquals(OffsetContextFactory.KEY + "[0]", contexts.get(0).identifier());
    }

    @Test
    public void testContextSeparate() {
        OffsetContextFactory<DepNode, DepTree> factory = new OffsetContextFactory<>(-1, 1);
        List<NlpContext<DepNode>> contexts = factory.apply(getTestInstance());
        assertEquals(2, contexts.size());
        assertEquals(1, contexts.get(0).tokens().size());
        assertEquals(OffsetContextFactory.KEY + "[-1]", contexts.get(0).identifier());
        assertEquals(OffsetContextFactory.KEY + "[1]", contexts.get(1).identifier());
        assertEquals(1, contexts.get(1).tokens().size());
        assertEquals(2, contexts.get(0).tokens().get(0).index());
        assertEquals(4, contexts.get(1).tokens().get(0).index());
    }

    @Test
    public void testContextConcatenated() {
        OffsetContextFactory<DepNode, DepTree> factory = new OffsetContextFactory<>(true, -1, 1);
        List<NlpContext<DepNode>> contexts = factory.apply(getTestInstance());
        assertEquals(1, contexts.size());
        assertEquals(OffsetContextFactory.KEY + "[-1,1]", contexts.get(0).identifier());
        assertEquals(2, contexts.get(0).tokens().size());
        assertEquals(2, contexts.get(0).tokens().get(0).index());
        assertEquals(4, contexts.get(0).tokens().get(1).index());
    }

}
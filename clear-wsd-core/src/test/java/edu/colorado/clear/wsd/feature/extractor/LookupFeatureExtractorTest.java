package edu.colorado.clear.wsd.feature.extractor;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import edu.colorado.clear.type.DepNode;
import edu.colorado.clear.type.FeatureType;
import edu.colorado.clear.wsd.type.DefaultDepNode;

import static junit.framework.TestCase.assertEquals;

/**
 * @author jamesgung
 */
public class LookupFeatureExtractorTest {

    @Test
    public void testLookup() {
        LookupFeatureExtractor<DepNode> lookupExtractor = new LookupFeatureExtractor<>(FeatureType.Text.name());
        DefaultDepNode depNode = new DefaultDepNode(0);
        depNode.addFeature(FeatureType.Text, "cat");
        String feat = lookupExtractor.extract(depNode);
        assertEquals("cat", feat);
    }

    @Test
    public void testFallback() {
        LookupFeatureExtractor<DepNode> lookupExtractor = new LookupFeatureExtractor<>(
                Collections.singletonList(FeatureType.Text.name()), new LookupFeatureExtractor<DepNode>(FeatureType.Lemma.name()));
        DefaultDepNode depNode = new DefaultDepNode(0);
        depNode.addFeature(FeatureType.Lemma, "cat");
        String feat = lookupExtractor.extract(depNode);
        assertEquals("cat", feat);
    }

    @Test
    public void testMultiple() {
        LookupFeatureExtractor<DepNode> lookupExtractor = new LookupFeatureExtractor<>(
                Arrays.asList(FeatureType.Text.name(), FeatureType.Lemma.name(), FeatureType.Pos.name()));
        DefaultDepNode depNode = new DefaultDepNode(0);
        depNode.addFeature(FeatureType.Lemma, "cat");
        depNode.addFeature(FeatureType.Pos, "NN");
        String feat = lookupExtractor.extract(depNode);
        assertEquals("cat", feat); // extract the value for the first key present
    }

}
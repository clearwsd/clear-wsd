package edu.colorado.clear.wsd.feature.extractor;

import org.junit.Test;

import java.util.Arrays;

import edu.colorado.clear.wsd.feature.util.FeatureUtils;
import edu.colorado.clear.wsd.type.BaseDepNode;
import edu.colorado.clear.wsd.type.DepNode;
import edu.colorado.clear.wsd.type.FeatureType;

import static junit.framework.TestCase.assertEquals;

/**
 * @author jamesgung
 */
public class ConcatenatingFeatureExtractorTest {

    @Test
    public void testConcatenate() {
        ConcatenatingFeatureExtractor<DepNode> extractor = new ConcatenatingFeatureExtractor<>(Arrays.asList(
                new LookupFeatureExtractor<>(FeatureType.Pos.name()),
                new LookupFeatureExtractor<>(FeatureType.Dep.name())));
        BaseDepNode depNode = new BaseDepNode(0);
        depNode.addFeature(FeatureType.Pos, "NN");
        depNode.addFeature(FeatureType.Dep, "nsubj");
        assertEquals(String.format("NN%snsubj", FeatureUtils.CONCAT_DELIM), extractor.extract(depNode));
    }

}
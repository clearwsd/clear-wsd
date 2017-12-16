package edu.colorado.clear.wsd.feature.pipeline;

import java.io.Serializable;
import java.util.List;

import edu.colorado.clear.type.NlpInstance;
import edu.colorado.clear.wsd.classifier.SparseInstance;
import edu.colorado.clear.wsd.feature.model.FeatureModel;

/**
 * Trainable feature pipeline.
 *
 * @param <I> input instance type
 * @author jamesgung
 */
public interface FeaturePipeline<I extends NlpInstance> extends Serializable {

    /**
     * Return the model associated with this feature pipeline.
     */
    FeatureModel model();

    /**
     * Compute features for a single instance (used at test time).
     *
     * @param inputInstance input instance
     * @return output instance (input to a classification algorithm)
     */
    SparseInstance process(I inputInstance);

    /**
     * Extract features and perform any necessary training-specific processing.
     *
     * @param instances list of input training instances
     * @return list of training instances (input to a classification algorithm)
     */
    List<SparseInstance> train(List<I> instances);

}

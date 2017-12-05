package edu.colorado.clear.wsd.classifier;

/**
 * Abstract classifier factory.
 *
 * @param <T> classifier type
 * @author jamesgung
 */
public interface ClassifierFactory<T extends Classifier> {

    /**
     * Instantiate a classifier.
     */
    T create();

}
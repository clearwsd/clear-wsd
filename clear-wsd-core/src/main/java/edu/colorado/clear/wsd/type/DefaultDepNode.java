package edu.colorado.clear.wsd.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.colorado.clear.type.DepNode;
import edu.colorado.clear.type.FeatureType;
import edu.colorado.clear.type.NlpInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Default {@link DepNode} implementation.
 *
 * @author jamesgung
 */
@Getter
@Accessors(fluent = true)
public class DefaultDepNode implements DepNode {

    private DepNode head;
    @Setter
    private NlpInstance nlpToken;
    @Setter
    private List<DepNode> children;

    public DefaultDepNode(int index) {
        this.nlpToken = new DefaultNlpInstance(index);
        children = new ArrayList<>();
    }

    @Override
    public String dep() {
        return feature(FeatureType.Dep);
    }

    @Override
    public int index() {
        return nlpToken.index();
    }

    public void head(DepNode depNode) {
        this.head = depNode;
        depNode.children().add(this);
    }

    @Override
    public Map<String, Object> features() {
        return nlpToken.features();
    }

    @Override
    public <T> T feature(FeatureType featureType) {
        return nlpToken.feature(featureType);
    }


    @Override
    public <T> T feature(String feature) {
        return nlpToken.feature(feature);
    }

    @Override
    public <T> void addFeature(FeatureType featureType, T value) {
        nlpToken.addFeature(featureType, value);
    }

    @Override
    public <T> void addFeature(String featureKey, T value) {
        nlpToken.addFeature(featureKey, value);
    }

    @Override
    public boolean isRoot() {
        return null == head;
    }

    @Override
    public String toString() {
        return nlpToken.toString();
    }
}

package org.jbpm.prediction.randomforest;

import java.util.Map;

abstract public class AbstractPredictionEngine {

    protected Map<String, FeatureType> inputFeatures;
    protected String outcomeFeatureName;
    protected FeatureType outcomeFeatureType;

    public AbstractPredictionEngine(Map<String, FeatureType> inputFeatures, String outputFeatureName, FeatureType outputFeatureType) {
        this.inputFeatures = inputFeatures;
        this.outcomeFeatureName = outputFeatureName;
        this.outcomeFeatureType = outputFeatureType;
    }

}

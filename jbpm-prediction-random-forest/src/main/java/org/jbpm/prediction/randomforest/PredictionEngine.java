package org.jbpm.prediction.randomforest;

import org.kie.internal.task.api.prediction.PredictionOutcome;

import java.util.Map;

public interface PredictionEngine {

    void train(Map<String, Object> inputData, Map<String, Object> outputData);
    PredictionOutcome predict(Map<String, Object> data);
}

package org.jbpm.prediction.randomforest.backends;

import org.jbpm.prediction.randomforest.AttributeType;
import org.kie.internal.task.api.prediction.PredictionOutcome;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PMMLRandomForestBackend extends AbstractPMMLBackend {
    public PMMLRandomForestBackend(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, File pmmlFile) {
        super(inputFeatures, outputFeatureName, outputFeatureType, pmmlFile);
    }

    @Override
    public PredictionOutcome predict(Map<String, Object> data) {
        Map<String, ?> result = evaluate(data);

        System.out.println(result);
        System.out.println(data.get("ActorId") + ", " + data.get("level") + ": " + result.get(outcomeFeatureName));

        Map<String, Object> outcomes = new HashMap<>();
        String predictionStr;
        Double confidence;

            Double prediction = (Double) result.get(outcomeFeatureName);
            confidence = Math.max(Math.abs(0.0 - prediction), Math.abs(1.0 - prediction));
            long predictionInt = Math.round(prediction);

            if (predictionInt == 0) {
                predictionStr = "false";
            } else {
                predictionStr = "true";
            }

        outcomes.put(outputFields.get(0).getFieldName().getValue(), predictionStr);
        outcomes.put("confidence", confidence);

        System.out.println(data + ", prediction = " + predictionStr + ", confidence = " + confidence);

        return new PredictionOutcome(confidence, 100, outcomes);
    }
}

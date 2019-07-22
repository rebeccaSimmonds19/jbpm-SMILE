package org.jbpm.prediction.randomforest.backends;

import org.jbpm.prediction.randomforest.AttributeType;
import org.kie.internal.task.api.prediction.PredictionOutcome;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PMMLLogisticRegressionBackend extends AbstractPMMLBackend {
    public PMMLLogisticRegressionBackend(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, File pmmlFile) {
        super(inputFeatures, outputFeatureName, outputFeatureType, pmmlFile);
    }

    @Override
    public PredictionOutcome predict(Map<String, Object> data) {
        Map<String, ?> result = evaluate(data);

        System.out.println(result);
        System.out.println(data.get("ActorId") + ", " + data.get("level") + ": " + result.get(outcomeFeatureName));

        Map<String, Object> outcomes = new HashMap<>();
        String predictionStr;
        Object predictionValue = result.get(outcomeFeatureName);
        Double confidence;
//        if (predictionValue instanceof Double) {
//            Double prediction = (Double) resultRecord.get(outcomeFeatureName);
//            confidence = Math.max(Math.abs(0.0 - prediction), Math.abs(1.0 - prediction));
//            long predictionInt = Math.round(prediction);
//
//            if (predictionInt == 0) {
//                predictionStr = "false";
//            } else {
//                predictionStr = "true";
//            }
//        } else {

            if ((Integer) predictionValue == 0) {
                confidence = (Double) result.get("probability_0");
                predictionStr = "false";
            } else {
                confidence = (Double) result.get("probability_1");
                predictionStr = "true";
            }

//        }

        outcomes.put(outputFields.get(0).getFieldName().getValue(), predictionStr);
        outcomes.put("confidence", confidence);

        System.out.println(data + ", prediction = " + predictionStr + ", confidence = " + confidence);

        return new PredictionOutcome(confidence, 100, outcomes);
    }
}

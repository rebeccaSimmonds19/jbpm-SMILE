package org.jbpm.prediction.randomforest.backends;

import org.dmg.pmml.FieldName;
import org.jbpm.prediction.randomforest.AbstractPredictionEngine;
import org.jbpm.prediction.randomforest.PredictionEngine;
import org.jbpm.prediction.randomforest.AttributeType;
import org.jpmml.evaluator.*;
import org.jpmml.evaluator.visitors.DefaultVisitorBattery;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PMMLBackend extends AbstractPredictionEngine implements PredictionEngine {

    private final Evaluator evaluator;
    private final List<? extends InputField> inputFields;
    private final List<? extends TargetField> targetFields;
    private final List<? extends OutputField> outputFields;

    public PMMLBackend(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, File pmmlFile) {
        super(inputFeatures, outputFeatureName, outputFeatureType);

        Evaluator _evalutator = null;
        try {
            _evalutator = new LoadingModelEvaluatorBuilder()
                    .setLocatable(false)
                    .setVisitors(new DefaultVisitorBattery())
                    .load(pmmlFile)
                    .build();
            _evalutator.verify();
        } catch (IOException | SAXException | JAXBException e) {
            e.printStackTrace();
        } finally {
            this.evaluator = _evalutator;
        }

        this.inputFields = evaluator.getInputFields();
        this.targetFields = evaluator.getTargetFields();
        this.outputFields = evaluator.getOutputFields();
    }

    @Override
    public void train(Map<String, Object> inputData, Map<String, Object> outputData) {

    }

    @Override
    public PredictionOutcome predict(Map<String, Object> data) {
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        for(InputField inputField : this.inputFields){

            FieldName inputName = inputField.getName();
            Object rawValue;

            // TODO: Automatically categorise features, remove hard-coding
            if (inputName.getValue().equals("ActorId")) {

                String strValue = (String) data.get(inputName.getValue());

                if (strValue.equals("john")) {
                    rawValue = 0;
                } else {
                    rawValue = 1;
                }

            } else {
                rawValue = data.get(inputName.getValue());
            }

            // Transforming an arbitrary user-supplied value to a known-good PMML value
            FieldValue inputValue = inputField.prepare(rawValue);

            arguments.put(inputName, inputValue);
        }

        // Evaluating the model with known-good arguments
        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        Map<String, ?> resultRecord = EvaluatorUtil.decodeAll(results);
        System.out.println(resultRecord);
        System.out.println(data.get("ActorId") + ", " + data.get("level") + ": " + resultRecord.get(outcomeFeatureName));

        Map<String, Object> outcomes = new HashMap<>();
        String predictionStr;
        Object predictionValue = resultRecord.get(outcomeFeatureName);
        Double confidence;
        if (predictionValue instanceof Double) {
            Double prediction = (Double) resultRecord.get(outcomeFeatureName);
            confidence = Math.max(Math.abs(0.0 - prediction), Math.abs(1.0 - prediction));
            long predictionInt = Math.round(prediction);

            if (predictionInt == 0) {
                predictionStr = "false";
            } else {
                predictionStr = "true";
            }
        } else {

            if ((Integer) predictionValue == 0) {
                confidence = (Double) resultRecord.get("probability_0");
                predictionStr = "false";
            } else {
                confidence = (Double) resultRecord.get("probability_1");
                predictionStr = "true";
            }

        }

        outcomes.put(outputFields.get(0).getFieldName().getValue(), predictionStr);
        outcomes.put("confidence", confidence);

        System.out.println(data + ", prediction = " + predictionStr + ", confidence = " + confidence);

        return new PredictionOutcome(confidence, 100, outcomes);
    }
}

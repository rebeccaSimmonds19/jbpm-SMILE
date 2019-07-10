package org.jbpm.prediction.randomforest.backends;

import org.jbpm.prediction.randomforest.AbstractPredictionEngine;
import org.jbpm.prediction.randomforest.FeatureType;
import org.jbpm.prediction.randomforest.PredictionEngine;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import smile.classification.NaiveBayes;
import smile.data.Attribute;
import smile.data.AttributeDataset;
import smile.data.NominalAttribute;

import java.text.ParseException;
import java.util.*;

public class SmileNaiveBayes extends AbstractPredictionEngine implements PredictionEngine {

    private final AttributeDataset dataset;
    private final Map<String, Attribute> smileAttributes;
    protected List<String> attributeNames = new ArrayList<>();
    private final Attribute outcomeAttribute;
    private NaiveBayes model = null;
    private Set<String> outcomeSet = new HashSet<>();
    private final int numAttributes;

    public SmileNaiveBayes(Map<String, FeatureType> inputFeatures, String outputFeatureName, FeatureType outputFeatureType) {
        super(inputFeatures, outputFeatureName, outputFeatureType);
        smileAttributes = new HashMap<>();
        for (Map.Entry<String, FeatureType> inputFeature : inputFeatures.entrySet()) {
            final String name = inputFeature.getKey();
            final FeatureType type = inputFeature.getValue();

            if (type == FeatureType.NOMINAL) {
                smileAttributes.put(name, new NominalAttribute(name));
                attributeNames.add(name);

            }
        }
        numAttributes = smileAttributes.size();

        if (outputFeatureType == FeatureType.NOMINAL) {
            outcomeAttribute = new NominalAttribute(outcomeFeatureName);
        } else {
            // only dealing with nominal features at the moment
            outcomeAttribute = new NominalAttribute(outcomeFeatureName);
        }


        dataset = new AttributeDataset("dataset", smileAttributes.values().toArray(new Attribute[numAttributes]), outcomeAttribute);
    }

    public void addData(Map<String, Object> data, Object outcome) {
        final double[] features = new double[numAttributes];
        int i = 0;
        for (String attrName : smileAttributes.keySet()) {
            try {
                features[i] = smileAttributes.get(attrName).valueOf(data.get(attrName).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            i++;
        }
        try {
            final String outcomeStr = outcome.toString();
            outcomeSet.add(outcomeStr);
            dataset.add(features, outcomeAttribute.valueOf(outcomeStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public void train(Map<String, Object> inputData, Map<String, Object> outputData) {
        addData(inputData, outputData.get(outcomeAttribute.getName()));
    }

    protected double[] buildFeatures(Map<String, Object> data) {
        final double[] features = new double[numAttributes];
        for (int i = 0 ; i < numAttributes ; i++) {
            final String attrName = attributeNames.get(i);
            try {
                features[i] = smileAttributes.get(attrName).valueOf(data.get(attrName).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return features;
    }


    public PredictionOutcome predict(Map<String, Object> data) {
        Map<String, Object> outcomes = new HashMap<>();
        if (outcomeSet.size() >= 2) {
//            model = new RandomForest(dataset, 100);
            model = new NaiveBayes(NaiveBayes.Model.POLYAURN, outcomeSet.size(), attributeNames.size());

            int[] y = new int[dataset.size()];
            for (int i = 0 ; i < dataset.size() ; i++) {

                y[i] = (int) dataset.get(i).y;
            }

            model.learn(dataset.x(), y);
            final double[] features = buildFeatures(data);
            final double[] posteriori = new double[outcomeSet.size()];
            double prediction = model.predict(features, posteriori);

            if (prediction != -1.0) {

                String predictionStr = dataset.responseAttribute().toString(prediction);
                outcomes.put(outcomeAttribute.getName(), predictionStr);
                final double confidence = posteriori[(int) prediction];
//            final double confidence = 0.0;
                outcomes.put("confidence", confidence);

                System.out.println(data + ", prediction = " + predictionStr + ", confidence = " + confidence);

                return new PredictionOutcome(0.0, 100.0, outcomes);
            } else {
                return new PredictionOutcome(0.0, 100.0, outcomes);
            }

        } else {
            return new PredictionOutcome(0.0, 100.0, outcomes);
        }
    }
}

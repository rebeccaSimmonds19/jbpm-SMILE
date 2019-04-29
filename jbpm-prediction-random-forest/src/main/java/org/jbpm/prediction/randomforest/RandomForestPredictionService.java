/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.prediction.randomforest;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import smile.classification.DecisionTree;
import smile.classification.RandomForest;
import smile.data.Attribute;
import smile.data.AttributeDataset;
import smile.data.NominalAttribute;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;


public class RandomForestPredictionService implements PredictionService {

    public static final String IDENTIFIER = "RandomForest";

    private double confidenceThreshold = 100.0;
    private int NUMBER_OF_TREES = 100;
    private int MIN_COUNT = 5;
    private int count = 0;

    // Random forest
    private RandomForest randomForest;
    private AttributeDataset dataset = null;

    private static NumberFormat formatter = new DecimalFormat("#0.00");

    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * Converts the normalised Out-Of-Bag error (OOB) into an accuracy measure.
     *
     * @param error An OOB error between 0 (minimum error) and 1 (maximum error).
     * @return An accuracy measure between 0% (minimum accuracy) and 100% (maximum accuracy)
     */
    private double accuracy(double error) {
        if (count < MIN_COUNT) {
            return 0D;
        }
        return (1.0 - error) * 100.0;
    }

    /**
     * Return the maximum of the label's posteriors for a given prediction
     *
     * @param posteriori An array with all the label's posterior
     * @return The maximum posterior (as percentage)
     */
    private double maxPosterior(double[] posteriori) {
        double max = posteriori[0];
        for (double v : posteriori) {
            max = Math.max(max, v);
        }
        return max * 100.0;
    }

    /**
     * Return all labels for the current dataset
     *
     * @return An array of int with all the labels
     */
    private static int[] getLabels(AttributeDataset dataset) {
        int[] ys = new int[dataset.size()];
        for (int i = 0; i < dataset.size(); i++) {
            ys[i] = (int) dataset.get(i).y;
        }
        return ys;
    }

    /**
     * Return unique labels in this dataset
     *
     * @return A Set containing the unique labels in this dataset
     */
    private static Set<Integer> getUniqueLabels(AttributeDataset dataset) {
        final Set<Integer> unique = new HashSet<>();
        for (int label : getLabels(dataset)) {
            unique.add(label);
        }
        return unique;
    }

    private static double[] buildFeatures(AttributeDataset dataset, Map<String, Object> inputData) throws ParseException {
        // remove extra information
        inputData.remove("TaskName");
        inputData.remove("NodeName");
        inputData.remove("Skippable");

        double[] features = new double[inputData.keySet().size()];

        int i = 0;
        for (String key : inputData.keySet()) {
            features[i] = dataset.attributes()[i].valueOf(String.valueOf(inputData.get(key)));
            i++;
        }
        return features;
    }

    /**
     * @param task
     * @param inputData
     * @return
     */
    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {

        if (randomForest == null) {
            return new PredictionOutcome();
        } else {
            try {
                double[] features = buildFeatures(dataset, inputData);
                // calculate accuracy as the maximum posterior for this prediction
                System.out.println(Arrays.toString(randomForest.importance()));
                double[] posteriori = new double[getUniqueLabels(dataset).size()];
                final int prediction = randomForest.predict(features, posteriori);
                double accuracy = maxPosterior(posteriori);

                Map<String, Object> outcomes = new HashMap<>();
                outcomes.put(dataset.responseAttribute().getName(), Boolean.valueOf(dataset.responseAttribute().toString(prediction)));
                outcomes.put("confidence", accuracy);
                outcomes.put("oob", accuracy(randomForest.error()));

                System.out.print(String.format("p(true)=%s, p(false)=%s, ", formatter.format(posteriori[0]), formatter.format(posteriori[1])));
                System.out.print("Input: actorId = " + inputData.get("ActorId") + ", item = " + inputData.get("item") + ", level = " + inputData.get("level"));
                System.out.println("; predicting '" + outcomes.get("approved") + "' with accuracy " + formatter.format(accuracy) + "%");
                return new PredictionOutcome(accuracy, confidenceThreshold, outcomes);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return new PredictionOutcome();
        }
    }

    private static void addToDataset(AttributeDataset dataset, Map<String, Object> inputData, Map<String, Object> outputData) throws ParseException {
        // remove extra information
        double[] features = buildFeatures(dataset, inputData);
        double response = dataset.responseAttribute().valueOf(outputData.entrySet().iterator().next().getValue().toString());

        dataset.add(features, response);
    }

    private AttributeDataset buildDataset(Map<String, Object> inputData, Map<String, Object> outputData) {
        // remove extra information
        inputData.remove("TaskName");
        inputData.remove("NodeName");
        inputData.remove("Skippable");
        System.out.println(inputData);
        Attribute[] attributes = new Attribute[inputData.keySet().size()];
        int index = 0;
        for (String key : inputData.keySet()) {
            attributes[index] = new NominalAttribute(key);
            index++;
        }

        Attribute outcome = new NominalAttribute(outputData.keySet().iterator().next());
        return new AttributeDataset("dataset", attributes, outcome);
    }

    /**
     * @param task
     * @param inputData  The input variables
     * @param outputData The task outcome
     */
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {

        count++;
        if (dataset==null) {
            dataset = buildDataset(inputData, outputData);
        }
        try {
            addToDataset(dataset, inputData, outputData);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        final Set<Integer> uniqueLabels = getUniqueLabels(dataset);
        if (uniqueLabels.size() >= 2) { // we have enough classes to perform a prediction
            randomForest = new RandomForest(dataset.attributes(), dataset.x(),
                    getLabels(dataset),
                    NUMBER_OF_TREES, 100, 5, 1, 0.5,
                    DecisionTree.SplitRule.ENTROPY);
        }
    }

}

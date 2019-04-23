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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import smile.classification.DecisionTree;
import smile.classification.RandomForest;
import smile.data.*;


public class RandomForestPredictionService implements PredictionService {
    
    public static final String IDENTIFIER = "RandomForest";
    
    private double confidenceThreshold = 100.0;
    private int NUMBER_OF_TREES = 100;
    private int MIN_COUNT = 5;
    private int count = 0;

    // Random forest
    private RandomForest randomForest;
    private Attribute user = new NominalAttribute("user");
    private Attribute level = new NominalAttribute("level");
    private Attribute approved = new NominalAttribute("approved");
    private AttributeDataset dataset = new AttributeDataset("test", new Attribute[]{user, level}, approved);

    private static NumberFormat formatter = new DecimalFormat("#0.00");

    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * Converts the normalised Out-Of-Bag error (OOB) into an accuracy measure.
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
     * @param posteriori An array with all the label's posterior
     * @return The maximum posterior (as percentage)
     */
    private double maxPosterior(double[] posteriori) {
        double max = posteriori[0];
        for (double v : posteriori) {
            max = Math.max(max, v);
        }
        return max*100.0;
    }

    /**
     * Return all labels for the current dataset
     * @return An array of int with all the labels
     */
    private int[] getLabels() {
        int[] ys = new int[dataset.size()];
        for (int i = 0 ; i < dataset.size() ; i++) {
            ys[i] = (int) dataset.get(i).y;
        }
        return ys;
    }

    /**
     * Return unique labels in this dataset
     * @return A Set containing the unique labels in this dataset
     */
    private Set<Integer> getUniqueLabels() {
        final Set<Integer> unique = new HashSet<>();
        for (int label : getLabels()) {
            unique.add(label);
        }
        return unique;
    }

    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {

        if (randomForest == null) {
            return new PredictionOutcome();
        } else {
            final double[] features;
            final String userValue = (String) inputData.get("ActorId");
            final int levelValue = (Integer) inputData.get("level");
            try {
                features = new double[]{
                        user.valueOf(userValue),
                        level.valueOf(String.valueOf(levelValue))
                };
                // calculate accuracy as the maximum posterior for this prediction
                System.out.println(Arrays.toString(randomForest.importance()));
                double[] posteriori = new double[getUniqueLabels().size()];
                final int prediction = randomForest.predict(features, posteriori);
                double accuracy = maxPosterior(posteriori);

                Map<String, Object> outcomes = new HashMap<>();
                outcomes.put("approved", Boolean.valueOf(approved.toString(prediction)));
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

    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {

    	count++;
        final String userValue = (String) inputData.get("ActorId");
        final int levelValue = (Integer) inputData.get("level");

        try {
            dataset.add(new double[]{
                    user.valueOf(userValue),
                    level.valueOf(String.valueOf(levelValue))
            }, approved.valueOf(outputData.get("approved").toString()));

        } catch (ParseException e) {
            e.printStackTrace();
        }
        final Set<Integer> uniqueLabels = getUniqueLabels();
        if (uniqueLabels.size() >= 2) { // we have enough classes to perform a prediction
            randomForest = new RandomForest(new Attribute[]{user, level}, dataset.x(),
                    getLabels(),
                    NUMBER_OF_TREES, 100, 5, 1, 0.5,
                    DecisionTree.SplitRule.ENTROPY);
        }
    }

}

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import smile.classification.DecisionTree;
import smile.classification.RandomForest;
import smile.data.*;


public class RandomForestPredictionService implements PredictionService {
    
    public static final String IDENTIFIER = "RandomForest";
    
    private double confidenceThreshold = 100.0;
    private int NUMBER_OF_TREES = 500;
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

    private double maxPosterior(double[] posteriori) {
        double max = posteriori[0];
        for (double v : posteriori) {
            max = Math.max(max, v);
        }
        return max*100.0;
    }

    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {
        String key = task.getName() + task.getTaskData().getDeploymentId()+ inputData.get("level");


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
                double[] posteriori = new double[features.length];
                final int prediction = randomForest.predict(features, posteriori);
                Map<String, Object> outcomes = new HashMap<>();
                double accuracy = maxPosterior(posteriori);
                outcomes.put("approved", Boolean.valueOf(approved.toString(prediction)));
                outcomes.put("confidence", accuracy);

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
        int[] ys = new int[dataset.size()];
        for (int i = 0 ; i < dataset.size() ; i++) {
            ys[i] = (int) dataset.get(i).y;
        }
        if (ys.length >= 2) { // we have enough classes to perform a prediction
//            System.out.println("================================================");
//            System.out.println(dataset.toString());
//            randomForest = new RandomForest(new Attribute[]{user, level}, dataset.x(), ys, NUMBER_OF_TREES, 100, 100, 2, 0.95, DecisionTree.SplitRule.GINI);
            randomForest = new RandomForest(dataset, NUMBER_OF_TREES);
//            System.out.println(dataset);
//            System.out.println(randomForest.getTrees()[200].dot());
        }
    }

}

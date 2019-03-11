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

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import smile.classification.RandomForest;
import smile.data.*;


public class RandomForestPredictionService implements PredictionService {
    
    public static final String IDENTIFIER = "RandomForest";
    
    private double confidenceThreshold = 90.0;
    
    // just for the sake of tests
    private Map<String, Boolean> predictions = new HashMap<>();
    private Map<String, Integer> predictionsConfidence = new HashMap<>();

    // Random forest
    private RandomForest randomForest;

    private Attribute userName = new StringAttribute("user");
    private Attribute level = new NumericAttribute("level");
    private Attribute approved = new NominalAttribute("approved");
    private AttributeDataset dataset = new AttributeDataset("test", new Attribute[]{userName, level}, approved);

    public String getIdentifier() {
        return IDENTIFIER;
    }

    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {
        String key = task.getName() + task.getTaskData().getDeploymentId()+ inputData.get("level");


        if (randomForest == null) {
            return new PredictionOutcome();
        } else {
            final double[] features = new double[]{};
            final int prediction = randomForest.predict(features);
            System.out.println("Prediction:");
            System.out.println(approved.toString(prediction));
            System.out.println("Error:");
            System.out.println(randomForest.error());
            Map<String, Object> outcomes = new HashMap<>();
            outcomes.put("approved", Boolean.valueOf(approved.toString(prediction)));
            outcomes.put("confidence", randomForest.error());
            return new PredictionOutcome(randomForest.error(), confidenceThreshold, outcomes);
        }
    }

    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {
        // training
        System.out.println("I'm inside `train`");
        // output values
        System.out.println("Input data:"); // {item=test item, TaskName=Task, NodeName=Task, level=5, Skippable=false, ActorId=john}
        System.out.println(inputData);
        System.out.println("Output data:"); // {approved=true, confidence=10.0, ActorId=john}
        System.out.println(outputData);
        System.out.println("Task:"); // TaskImpl [id=2, name=Task]
        System.out.println(task);

        // add to dataset

        try {
            dataset.add(new double[]{
                    userName.valueOf((String) inputData.get("ActorId")),
                    level.valueOf(inputData.get("level").toString())
            }, approved.valueOf(outputData.get("approved").toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int[] ys = new int[dataset.size()];
        for (int i = 0 ; i < dataset.size() ; i++) {
            ys[i] = (int) dataset.get(i).y;
        }
        if (dataset.size() <= 2) {

        } else {
            randomForest = new RandomForest(dataset.x(), ys, 100);
        }

    }

}

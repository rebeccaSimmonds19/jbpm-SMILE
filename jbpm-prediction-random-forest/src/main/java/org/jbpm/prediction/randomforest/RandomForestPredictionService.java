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
import smile.data.Attribute;
import smile.data.NominalAttribute;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;


public class RandomForestPredictionService implements PredictionService {

    public static final String IDENTIFIER = "RandomForest";

    private double confidenceThreshold = 100.0;
    private PredictionEngine engine = null;

    private static NumberFormat formatter = new DecimalFormat("#0.00");


    private void buildEngine() {
        if (engine==null) {

            // Use SMILE Random Forests as the prediction engine

            Attribute item = new NominalAttribute("item");
            Attribute actorId = new NominalAttribute("ActorId");
            Attribute level = new NominalAttribute("level");
            Attribute approved = new NominalAttribute("approved");
            List<Attribute> inputs = new ArrayList<>();
            inputs.add(item);
            inputs.add(actorId);
            inputs.add(level);

            engine = new SmileNaiveBayes(inputs, approved);
        }
    }

    // Random forest
    private RandomForest randomForest;

    private Attribute userName = new StringAttribute("user");
    private Attribute level = new NumericAttribute("level");
    private Attribute approved = new NominalAttribute("approved");
    private AttributeDataset dataset = new AttributeDataset("test", new Attribute[]{userName, level}, approved);

    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * @param task
     * @param inputData
     * @return
     */
    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {

        if (engine==null) {
            buildEngine();
        }
        return engine.predict(inputData);
    }

    /**
     * @param task
     * @param inputData  The input variables
     * @param outputData The task outcome
     */
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {

       if (engine==null) {
           buildEngine();
       }

        engine.train(inputData, outputData);
    }

}

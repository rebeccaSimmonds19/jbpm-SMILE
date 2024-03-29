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

import org.jbpm.prediction.randomforest.backends.AbstractPMMLBackend;
import org.jbpm.prediction.randomforest.backends.PMMLLogisticRegressionBackend;
import org.jbpm.prediction.randomforest.backends.PMMLRandomForestBackend;
import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class RandomForestPredictionService implements PredictionService {

    public static final String IDENTIFIER = "RandomForest";

    private double confidenceThreshold = 100.0;
    private PredictionEngine engine = null;

    private void buildEngine() {
        if (engine==null) {

            final Map<String, AttributeType> inputFeatures = new HashMap<>();

            inputFeatures.put("item", AttributeType.NOMINAL);
            inputFeatures.put("ActorId", AttributeType.NOMINAL);
            inputFeatures.put("level", AttributeType.NOMINAL);

//          engine = new SmileRandomForest(inputFeatures, "approved", FeatureType.NOMINAL);
//          engine = new SmileNaiveBayes(inputFeatures, "approved", FeatureType.NOMINAL);

//            File pmmlFile = new File(getClass().getClassLoader().getResource("models/logistic_regression.pmml").getFile());
            File pmmlFile = new File(getClass().getClassLoader().getResource("models/random_forest.pmml").getFile());

            engine = new PMMLLogisticRegressionBackend(inputFeatures, "approved", AttributeType.NOMINAL, pmmlFile);
            engine = new PMMLRandomForestBackend(inputFeatures, "approved", AttributeType.NOMINAL, pmmlFile);
        }
    }

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

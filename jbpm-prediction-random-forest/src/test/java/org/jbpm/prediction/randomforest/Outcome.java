package org.jbpm.prediction.randomforest;

import org.jbpm.prediction.randomforest.annotations.AttributeType;
import org.jbpm.prediction.randomforest.annotations.RandomForestFeature;

public class Outcome {

    @RandomForestFeature(type= AttributeType.NOMINAL)
    private final String user;
    @RandomForestFeature(type= AttributeType.NOMINAL)
    private final int level;

    private Outcome(String user, int level) {
        this.user = user;
        this.level = level;
    }

    public String getUser() {
        return user;
    }

    public int getLevel() {
        return level;
    }

    public static Outcome create(String user, int level) {
        return new Outcome(user, level);
    }
}

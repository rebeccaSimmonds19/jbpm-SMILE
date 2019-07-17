package org.jbpm.prediction.randomforest.annotations;

import org.jbpm.prediction.randomforest.AttributeType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RandomForestFeature {
    AttributeType type() default AttributeType.NOMINAL;
}

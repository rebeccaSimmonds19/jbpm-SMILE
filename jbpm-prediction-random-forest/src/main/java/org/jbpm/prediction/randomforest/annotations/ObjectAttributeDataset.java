package org.jbpm.prediction.randomforest.annotations;

import smile.data.*;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ObjectAttributeDataset<R> {

    private Attribute[] attributes = new Attribute[]{};
    private Attribute responseAttribute = null;
    private AttributeDataset dataset = null;
    private final String name;
    private final String responseName;
    private boolean initialised = false;

    public ObjectAttributeDataset(String name, String responseName) {
        this.name = name;
        this.responseName = responseName;
    }

    private final Attribute[] extractAnnotatedTypes(Object object) {
        List<Attribute> fieldTypes = new ArrayList<>();
        for(Field field  : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(RandomForestFeature.class)) {
                Attribute attribute;
                final RandomForestFeature annotation = field.getAnnotation(RandomForestFeature.class);
                switch (annotation.type()) {
                    case STRING:
                        attribute = new StringAttribute(field.getName());
                        break;
                    case NUMERIC:
                        attribute = new NumericAttribute(field.getName());
                        break;
                    default:
                        attribute = new NominalAttribute(field.getName());
                }
                fieldTypes.add(attribute);
            }
        }
        return fieldTypes.toArray(new Attribute[0]);
    }

    public void add(Object object, R response) throws IllegalAccessException, ParseException {
        if (!initialised) {
            attributes = extractAnnotatedTypes(object);
            responseAttribute = new NominalAttribute(responseName);
            dataset = new AttributeDataset(name, attributes, responseAttribute);
            initialised = true;
        }

        Field[] fields = object.getClass().getDeclaredFields();
        final int k = fields.length;
        final double[] features = new double[attributes.length];
        int index = 0;
        for (int i = 0 ; i < k ; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            if (field.isAnnotationPresent(RandomForestFeature.class)) {
                Object value = field.get(object);
                if (attributes[index].getType() == Attribute.Type.NUMERIC) {
                    features[index] = Double.valueOf(value.toString());
                } else {
                    features[index] = attributes[index].valueOf(value.toString());
                }
                index++;
            }
        }
        dataset.add(features, (int) responseAttribute.valueOf(response.toString()));
    }

    public AttributeDataset getDataset() {
        return dataset;
    }
}

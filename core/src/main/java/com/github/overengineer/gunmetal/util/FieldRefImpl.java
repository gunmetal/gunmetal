package com.github.overengineer.gunmetal.util;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;

/**
 * @author rees.byars
 */
public class FieldRefImpl implements FieldRef {

    private transient volatile SoftReference<Field> fieldRef;
    private final String fieldName;
    private final Class<?> fieldDeclarer;

    public FieldRefImpl(Field field) {
        field.setAccessible(true);
        fieldRef = new SoftReference<Field>(field);
        fieldName = field.getName();
        fieldDeclarer = field.getDeclaringClass();
    }

    @Override
    public Field getField() {
        Field field = fieldRef == null ? null : fieldRef.get();
        if (field == null) {
            synchronized (this) {
                field = fieldRef == null ? null : fieldRef.get();
                if (field == null) {
                    try {
                        field = fieldDeclarer.getField(fieldName);
                        field.setAccessible(true);
                        fieldRef = new SoftReference<Field>(field);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return field;
    }
}

package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.util.FieldProxy;

/**
 * @author rees.byars
 */
public class CircularReferenceException  extends RuntimeException {

    private Class<?> componentType;
    private Object qualifier;
    private ComponentStrategy<?> reverseStrategy;
    private FieldProxy fieldProxy;

    protected CircularReferenceException(String message) {
        super(message);
    }

    protected CircularReferenceException(Class<?> componentType, Object qualifier) {
        super("The component of type [" + componentType.getName() + "] with qualifier [" + qualifier + "] was requested by a descendant dependency");
        this.componentType = componentType;
        this.qualifier = qualifier;
    }

    public Class<?> getComponentType() {
        return componentType;
    }

    public Object getQualifier() {
        return qualifier;
    }

    public void setReverseStrategy(ComponentStrategy<?> reverseStrategy) {
        this.reverseStrategy = reverseStrategy;
    }

    public ComponentStrategy<?> getReverseStrategy() {
        return reverseStrategy;
    }

    public void setFieldProxy(FieldProxy fieldProxy) {
        this.fieldProxy = fieldProxy;
    }

    public FieldProxy getFieldProxy() {
        return fieldProxy;
    }

}

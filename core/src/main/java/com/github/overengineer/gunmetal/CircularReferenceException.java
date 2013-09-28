package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public class CircularReferenceException  extends RuntimeException {

    private static final long serialVersionUID = 5969611824229305011L;
    private Class<?> componentType;
    private Object qualifier;
    private ComponentStrategy<?> reverseStrategy;

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

    @Override
    public String getMessage() {
        if (reverseStrategy != null) {
            return super.getMessage() + " of type [" + reverseStrategy.getComponentType() + "]";
        }
        return super.getMessage();
    }

}

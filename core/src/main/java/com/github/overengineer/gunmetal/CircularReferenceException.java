package com.github.overengineer.gunmetal;

import java.util.Stack;

/**
 * @author rees.byars
 */
public class CircularReferenceException  extends RuntimeException {

    private Class<?> componentType;
    private Object qualifier;
    private ComponentStrategy<?> reverseStrategy;
    private Stack<TargetAccessor> targetAccessors = new Stack<TargetAccessor>();

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

    public void addAccessor(TargetAccessor targetAccessor) {
        targetAccessors.add(targetAccessor);
    }

    public TargetAccessor getTargetAccessor() {
        if (targetAccessors.empty()) {
            throw new CircularReferenceException(getMessage() + ".  This dependency cycle could likely be resolved if no more than one link in a row in the chain/cycle is a constructor dependency.  Alternating constructor and setter dependencies can be resolved.");
        }
        return targetAccessors.pop();
    }

    public interface TargetAccessor {
        Object getTarget(Object reverseComponent);
    }

    @Override
    public String getMessage() {
        if (reverseStrategy != null) {
            return super.getMessage() + " of type [" + reverseStrategy.getComponentType() + "]";
        }
        return super.getMessage();
    }
}

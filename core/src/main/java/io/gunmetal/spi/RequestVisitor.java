package io.gunmetal.spi;

/**
 * @author rees.byars
 */
public interface RequestVisitor {

    RequestVisitor NONE = (dependencyRequest, errors) -> {
    };

    void visit(DependencyRequest dependencyRequest, ProvisionErrors errors);

}

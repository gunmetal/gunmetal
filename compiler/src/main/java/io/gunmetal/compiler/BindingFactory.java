package io.gunmetal.compiler;

import io.gunmetal.Provider;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor8;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rees.byars
 */
class BindingFactory implements Factory<Binding> {

    private final Factory<MemberMetadata> memberMetadataFactory;
    private final Factory<ProviderLocation> providerLocationFactory;
    private final Provider<Builder<Qualifier>> qualifierBuilderProvider;

    BindingFactory(
            Factory<MemberMetadata> memberMetadataFactory,
            Factory<ProviderLocation> providerLocationFactory,
            Provider<Builder<Qualifier>> qualifierBuilderProvider) {
        this.memberMetadataFactory = memberMetadataFactory;
        this.providerLocationFactory = providerLocationFactory;
        this.qualifierBuilderProvider = qualifierBuilderProvider;
    }

    @Override public Binding create(Element providerElement) {

        ExecutableReport report = new ExecutableReport(providerElement);

        ProviderLocation location = providerLocationFactory.create(providerElement);
        MemberMetadata providerMetadata = memberMetadataFactory.create(providerElement);
        Qualifier mergedQualifier = location.metadata().qualifier().merge(providerMetadata.qualifier());
        // TODO should this be handled in the memberMetadataFactory?
        providerMetadata = new MemberMetadata(mergedQualifier, providerMetadata.scope(), providerElement);

        Dependency fulfilledDependency = new Dependency(report.producedType, providerMetadata.qualifier());

        List<Dependency> requiredDependencies = new ArrayList<>();
        for (VariableElement parameterElement : report.parameterElements) {
            Builder<Qualifier> qualifierBuilder = qualifierBuilderProvider.get();
            new AnnotatedElement(parameterElement).accept(qualifierBuilder);
            requiredDependencies.add(new Dependency(parameterElement.asType(), qualifierBuilder.build()));
        }

        // TODO separate instance vs static from constructor vs method?
        // For instance methods and non-static inner class constructors, the provider location
        // is an instance, hence it is an additional dependency
        Dependency providerInstanceDependency = null;
        ProviderKind providerKind = ProviderKind.fromElement(providerElement);
        if (providerKind == ProviderKind.INSTANCE_CONSTRUCTOR
                || providerKind == ProviderKind.INSTANCE_METHOD) {
            providerInstanceDependency = new Dependency(
                    location.metadata().element().asType(), providerMetadata.qualifier());
        }

        return new Binding(
                providerMetadata,
                providerKind,
                location,
                fulfilledDependency,
                requiredDependencies,
                providerInstanceDependency);
    }

    private static class ExecutableReport {

        TypeMirror producedType;

        List<? extends VariableElement> parameterElements;

        ExecutableReport(Element element) {

            element.accept(new ElementKindVisitor8<ExecutableElement, Void>() {

                @Override public ExecutableElement visitExecutableAsConstructor(
                        ExecutableElement constructorElement, Void v) {

                    // TODO what to do about non-static inner class?  treat same as instance method?

                    producedType = constructorElement.getEnclosingElement().asType();
                    parameterElements = constructorElement.getParameters();
                    return null;
                }

                @Override public ExecutableElement visitExecutableAsMethod(
                        ExecutableElement methodElement, Void p) {

                    // TODO for instance method, what needs to be done to resolve instance?

                    producedType = methodElement.getReturnType();
                    parameterElements = methodElement.getParameters();
                    return null;
                }

            }, null);

        }

    }

}

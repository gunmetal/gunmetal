package io.gunmetal.compiler;

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
class Provider implements GraphMember {

    private final MemberMetadata memberMetadata;
    private final ProviderKind kind;
    private final ProviderLocation location;
    private final Dependency fulfilledDependency;
    private final List<Dependency> requiredDependencies;

    Provider(
            MemberMetadata memberMetadata,
            ProviderKind kind,
            ProviderLocation location,
            Dependency fulfilledDependency, List<Dependency> requiredDependencies) {
        this.memberMetadata = memberMetadata;
        this.kind = kind;
        this.location = location;
        this.fulfilledDependency = fulfilledDependency;
        this.requiredDependencies = requiredDependencies;
    }

    static Provider fromElement(Element providerElement) {

        ExecutableReport report = new ExecutableReport(providerElement);

        ProviderLocation location = ProviderLocation.fromElement(providerElement);
        MemberMetadata providerMetadata = MemberMetadata.fromElement(providerElement);
        Qualifier mergedQualifier = location.metadata().qualifier().merge(providerMetadata.qualifier());
        providerMetadata = new MemberMetadata(mergedQualifier, providerMetadata.scope());

        Dependency fulfilledDependency = new Dependency(report.producedType, providerMetadata.qualifier());

        List<Dependency> requiredDependencies = new ArrayList<>();
        for (VariableElement parameterElement : report.parameterElements) {
            // TODO only need qualifier, dis is bad :)
            MemberMetadata metadata = MemberMetadata.fromElement(parameterElement);
            requiredDependencies.add(new Dependency(parameterElement.asType(), metadata.qualifier()));
        }

        return new Provider(
                providerMetadata,
                ProviderKind.fromElement(providerElement),
                location,
                fulfilledDependency, requiredDependencies);

    }

    @Override public MemberMetadata metadata() {
        return memberMetadata;
    }

    ProviderKind kind() {
        return kind;
    }

    ProviderLocation location() {
        return location;
    }

    Dependency fulfilledDependency() {
        return fulfilledDependency;
    }

    List<Dependency> requiredDependencies() {
        return requiredDependencies;
    }

    private static class ExecutableReport {

        TypeMirror producedType;

        List<? extends VariableElement> parameterElements;

        ExecutableReport(Element element) {

            element.accept(new ElementKindVisitor8<ExecutableElement, Void>() {

                @Override public ExecutableElement visitExecutableAsConstructor(
                        ExecutableElement constructorElement, Void v) {
                    producedType = constructorElement.getEnclosingElement().asType();
                    parameterElements = constructorElement.getParameters();
                    return null;
                }

                @Override public ExecutableElement visitExecutableAsMethod(
                        ExecutableElement methodElement, Void p) {
                    producedType = methodElement.getReturnType();
                    parameterElements = methodElement.getParameters();
                    return null;
                }

            }, null);

        }

    }

}

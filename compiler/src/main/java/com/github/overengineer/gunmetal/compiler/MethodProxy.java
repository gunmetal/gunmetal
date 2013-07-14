package com.github.overengineer.gunmetal.compiler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * @author rees.byars
 */
public class MethodProxy {

    private final List<? extends TypeMirror> parameterTypeMirrors;

    private MethodProxy(ExecutableType executableType) {
        parameterTypeMirrors = executableType.getParameterTypes();
    }

    boolean canBeImplementedBy(Element implementationMethodElement) {
        List<? extends TypeMirror> implementationParameterTypeMirrors =
                from(implementationMethodElement).parameterTypeMirrors;
        for (int i = 0; i < parameterTypeMirrors.size(); i++) {
            TypeMirror apiParamterTypeMirror = parameterTypeMirrors.get(i);
            TypeMirror implementationParamterTypeMirror = implementationParameterTypeMirrors.get(i);
            if (!apiParamterTypeMirror.toString().equals(implementationParamterTypeMirror.toString())) {
                return false;
            }
        }
        return true;
    }

    static MethodProxy from(Element element) {
        if (element.getKind() != ElementKind.METHOD) {
            throw new IllegalArgumentException("The element [" + element + "] is not of kind METHOD");
        }
        return element.asType().accept(new AbstractTypeVisitor<MethodProxy, Void>() {
            @Override
            public MethodProxy visitExecutable(ExecutableType executableType, Void v) {
                return new MethodProxy(executableType);
            }
        }, null);
    }

}

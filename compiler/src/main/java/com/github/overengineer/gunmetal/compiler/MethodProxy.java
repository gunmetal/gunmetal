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

    private final ExecutableType executableType;

    private MethodProxy(ExecutableType executableType) {
        this.executableType = executableType;
    }

    boolean canBeImplementedBy(Element implementationMethodElement) {
        MethodProxy implementationMethodDescriptor = from(implementationMethodElement);
        List<? extends TypeMirror> apiParameterTypeMirrors = executableType.getParameterTypes();
        List<? extends TypeMirror> implementationParameterTypeMirrors = implementationMethodDescriptor.executableType.getParameterTypes();
        for (int i = 0; i < apiParameterTypeMirrors.size(); i++) {
            TypeMirror apiParamterTypeMirror = apiParameterTypeMirrors.get(i);
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
        return element.asType().accept(new MethodVisitor(), element);
    }

    static class MethodVisitor extends AbstractTypeVisitor<MethodProxy, Element> {
        @Override
        public MethodProxy visitExecutable(ExecutableType executableType, Element element) {
            return new MethodProxy(executableType);
        }
    }

}

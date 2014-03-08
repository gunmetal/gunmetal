package com.github.overengineer.gunmetal.compiler;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import java.util.HashSet;
import java.util.Set;

/**
 * @author rees.byars
 */
public class ImplementationVisitor extends AbstractTypeVisitor<ImplementationVisitor.Report, Element> {

    @Override
    public Report visitPrimitive(PrimitiveType primitiveType, Element element) {
        Report report = new Report();
        report.declaredType = false;
        return report;
    }

    @Override
    public Report visitArray(ArrayType arrayType, Element element) {
        Report report = new Report();
        report.declaredType = false;
        return report;
    }

    @Override
    public Report visitDeclared(DeclaredType declaredType, Element apiMethod) {
        Report report = new Report();
        report.declaredType = true;
        Element implementationElement = declaredType.asElement();
        report.finalImplementation = implementationElement.getModifiers().contains(Modifier.FINAL);
        MethodProxy apiMethodProxy = MethodProxy.from(apiMethod);
        for (Element childElement : implementationElement.getEnclosedElements()) {
            if (childElement.getKind() == ElementKind.METHOD
                    && childElement.getModifiers().contains(Modifier.PUBLIC)
                    && !childElement.getModifiers().contains(Modifier.ABSTRACT)
                    && childElement.getSimpleName().equals(apiMethod.getSimpleName())
                    && apiMethodProxy.canBeImplementedBy(childElement)) {
                report.matchingMethods.add(childElement);
            }
        }
        return report;
    }

    public static class Report {

        boolean declaredType;
        boolean finalImplementation;
        Set<Element> matchingMethods = new HashSet<>();

    }

}

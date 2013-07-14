package com.github.overengineer.gunmetal.compiler;

import com.github.overengineer.gunmetal.metadata.DeconstructedApi;
import com.github.overengineer.gunmetal.metadata.ImplementedBy;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * @author rees.byars
 */
@SupportedAnnotationTypes("com.github.overengineer.gunmetal.metadata.DeconstructedApi")
public class DeconstructedApiProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, final RoundEnvironment roundEnvironment) {
        boolean result = true;
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(DeconstructedApi.class);
        for (final Element classElement : elements) {
            if (classElement.getKind() != ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "The class [" + classElement + "] has an @" + DeconstructedApi.class.getSimpleName() + " but it is not an interface");
                return false;
            }
            TypeElement typeElement = (TypeElement) classElement;
            for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
                result = interfaceMirror.accept(new SimpleTypeVisitor6<Boolean, Void>() {
                    public Boolean visitDeclared(DeclaredType interfaceType, Void v) {
                        for (Element interfaceChild : interfaceType.asElement().getEnclosedElements()) {
                            if (interfaceChild.getKind() == ElementKind.METHOD) {
                                processingEnv.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "The Deconstructed API [" + classElement + "] illegally extends another interface that contains methods [" + interfaceChild + "]");
                                return false;
                            }
                        }
                        return true;
                    }
                }, null);
            }
            for (Element childElement : classElement.getEnclosedElements()) {
                if (childElement.getKind() == ElementKind.METHOD) {
                    ImplementedBy implementedBy = childElement.getAnnotation(ImplementedBy.class);
                    if (implementedBy == null) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "The class [" + classElement + "] has an @" + DeconstructedApi.class.getSimpleName() + " annotation but the method [" + childElement.getSimpleName() + "] does not have the @" + ImplementedBy.class.getSimpleName() + " annotation");
                        result = false;
                    }
                }
            }
        }
        return result;
    }

}

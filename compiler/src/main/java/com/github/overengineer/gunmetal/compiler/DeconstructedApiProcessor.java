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
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(DeconstructedApi.class);
        for (Element classElement : elements) {
            if (classElement.getKind() != ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "The class [" + classElement + "] has an @" + DeconstructedApi.class.getSimpleName() + " but it is not an interface");
                return false;
            }
            for (Element childElement : classElement.getEnclosedElements()) {
                if (childElement.getKind() == ElementKind.METHOD) {
                    ImplementedBy implementedBy = childElement.getAnnotation(ImplementedBy.class);
                    if (implementedBy == null) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "The class [" + classElement + "] has an @" + DeconstructedApi.class.getSimpleName() + " annotation but the method [" + childElement.getSimpleName() + "] does not have the @" + ImplementedBy.class.getSimpleName() + " annotation");
                        return false;
                    }
                }
            }
        }
        return true;
    }

}

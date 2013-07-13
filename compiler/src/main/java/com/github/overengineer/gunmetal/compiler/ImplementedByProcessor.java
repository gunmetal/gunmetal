package com.github.overengineer.gunmetal.compiler;

import com.github.overengineer.gunmetal.metadata.DeconstructedApi;
import com.github.overengineer.gunmetal.metadata.ImplementedBy;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * @author rees.byars
 */
@SupportedAnnotationTypes("com.github.overengineer.gunmetal.metadata.ImplementedBy")
public class ImplementedByProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ImplementedBy.class);
        boolean result = true;
        for (Element methodElement : elements) {
            Element classElement = methodElement.getEnclosingElement();
            DeconstructedApi deconstructedApi = classElement.getAnnotation(DeconstructedApi.class);
            if (deconstructedApi == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "The class [" + classElement + "] has @" + ImplementedBy.class.getSimpleName() + " annotations but does not have the @" + DeconstructedApi.class.getSimpleName() + " annotation");
                result = false;
            }
            ImplementedBy implementedBy = methodElement.getAnnotation(ImplementedBy.class);
            try {
                implementedBy.value();
            } catch (MirroredTypeException e) {
                TypeMirror typeMirror = e.getTypeMirror();
                ImplementationVisitor.Report report = typeMirror.accept(new ImplementationVisitor(), methodElement);
                if (!report.declaredType) {
                    result = false;
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "The type [" + typeMirror + "] is not valid.  Class cannot be an array or primitive type.");
                }
                if (!report.finalImplementation) {
                    result = false;
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "The type [" + typeMirror + "] is not final.  API delegates must be final.");
                }
                if (report.matchingMethods.size() > 1) {
                    result = false;
                    StringBuilder b = new StringBuilder();
                    b.append("\nMatching methods:");
                    for (Element m : report.matchingMethods) {
                        b.append("\n");
                        b.append(m);
                    }
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "The type [" + typeMirror + "] has more than one method that can implement the API method [" + methodElement + "]" + b.toString());
                }
                if (report.matchingMethods.size() == 0) {
                    result = false;
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "The type [" + typeMirror + "] has no methods that can implement the API method [" + methodElement + "]");
                }
            }
        }
        return result;
    }

}

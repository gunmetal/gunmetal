package io.gunmetal;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

/**
 * @author rees.byars
 */
@SupportedAnnotationTypes("io.gunmetal.Provides")
public class ProviderProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> providesElements = roundEnv.getElementsAnnotatedWith(Provides.class);

        Set<Provider> providers = new HashSet<>();

        for (Element providerElement : providesElements) {

            Provider provider = Provider.fromElement(providerElement);
            providers.add(provider);


        }

        return false;
    }

}

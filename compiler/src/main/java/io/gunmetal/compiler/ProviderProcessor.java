package io.gunmetal.compiler;

import io.gunmetal.Provides;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;
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

        Map<Dependency, Provider> providers = new HashMap<>();

        for (Element providerElement : providesElements) {

            Provider provider = Provider.fromElement(providerElement);
            providers.put(provider.fulfilledDependency(), provider);


        }

        for (Provider provider : providers.values()) {
            for (Dependency dependency : provider.requiredDependencies()) {
                Provider dependencyProvider = providers.get(dependency);
                if (dependencyProvider == null) {
                    throw new RuntimeException();
                }
            }
        }

        return false;
    }

}

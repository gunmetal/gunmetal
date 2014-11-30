package io.gunmetal.compiler;

import com.squareup.javawriter.JavaWriter;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * @author rees.byars
 */
class ProviderWriter {

    // TODO this is a complete, non-working hack ;)
    private final ProviderNameResolver nameResolver = new ProviderNameResolver();
    private final Filer filer;

    ProviderWriter(
            Filer filer) {
        this.filer = filer;
    }

    void writeProviderFor(Binding binding) throws IOException {

        String typeName = nameResolver.getProviderNameFor(binding.fulfilledDependency());

        JavaFileObject javaFileObject = filer.createSourceFile(typeName);
        JavaWriter javaWriter = new JavaWriter(javaFileObject.openWriter());

        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            javaWriter.emitPackage(typeName.substring(0, nameIndex));
        }

        // TODO write qualifier annotation?

        javaWriter
                .beginType(typeName, "class", EnumSet.of(PUBLIC, FINAL))
                .beginConstructor(EnumSet.of(PUBLIC))
                .endConstructor()
                .endType()
                .close();
    }

    private static class ProviderNameResolver {

        private final Map<Dependency, String> providerNames = new HashMap<>();

        private String getProviderNameFor(Dependency dependency) {
            String name = providerNames.get(dependency);
            if (name != null) {
                return name;
            }
            String typeName = dependency.typeMirror().toString();
            int nameIndex = typeName.lastIndexOf(".");
            if (nameIndex > 0) {
                // Strip special chars for generics, etc
                String endName = typeName
                        .substring(nameIndex)
                        .replaceAll("[^A-Za-z0-9]", "");
                String packageName = typeName.substring(0, nameIndex);
                typeName = packageName + "." + endName;
            }
            return getProviderNameFor(dependency, typeName + "_$Provider", 0);
        }

        private String getProviderNameFor(Dependency dependency, String providerName, int index) {
            if (index != 0) {
                providerName += index;
            }
            if (providerNames.values().contains(providerName)) {
                return getProviderNameFor(dependency, providerName, index + 1);
            }
            providerNames.put(dependency, providerName);
            return providerName;
        }

    }

}

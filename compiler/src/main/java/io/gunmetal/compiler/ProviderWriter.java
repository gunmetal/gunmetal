package io.gunmetal.compiler;

import com.squareup.javawriter.JavaWriter;
import io.gunmetal.Provider;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.beans.Introspector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
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

        List<Dependency> requiredDependencies = binding.requiredDependencies();
        writeImportsForRequiredDependencies(javaWriter, requiredDependencies);
        // TODO write qualifier annotation?
        // TODO implements provider
        javaWriter.beginType(typeName, "class", EnumSet.of(PUBLIC, FINAL));
        // TODO should all be providers
        writeFieldsForRequiredDependencies(javaWriter, binding.requiredDependencies());
        writeConstructorForRequiredDependencies(javaWriter, binding.requiredDependencies());
        javaWriter.endType().close();

    }

    void writeImportsForRequiredDependencies(
            JavaWriter javaWriter, List<Dependency> dependencies) throws IOException {
        List<String> imports = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            // TODO will break on generic types
            imports.add(dependency.typeMirror().toString());
        }
        imports.add(Provider.class.getName());
        javaWriter.emitImports(imports);
        javaWriter.emitEmptyLine();
    }

    void writeFieldsForRequiredDependencies(
            JavaWriter javaWriter, List<Dependency> dependencies) throws IOException {
        for (Dependency dependency : dependencies) {
            javaWriter.emitField(
                    getSimpleTypeName(dependency),
                    getFieldNameForDependency(dependency));
        }
    }

    void writeConstructorForRequiredDependencies(
            JavaWriter javaWriter, List<Dependency> dependencies) throws IOException {
        List<String> parameters = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            parameters.add(getSimpleTypeName(dependency));
            parameters.add(getFieldNameForDependency(dependency));
        }
        javaWriter
                .beginConstructor(EnumSet.of(PUBLIC), parameters, Collections.<String>emptyList());
        for (Dependency dependency : dependencies) {
            String fieldName = getFieldNameForDependency(dependency);
            String statement =
                    "this." + fieldName
                    + " = " + fieldName;
            javaWriter.emitStatement(statement);
        }
        javaWriter.endConstructor();
    }

    // TODO the names should use caching strategy similar to the ProviderNameResolver but per-binding

    private String getFieldNameForDependency(Dependency dependency) {
        return Introspector.decapitalize(clean(getSimpleTypeName(dependency)));
    }

    private static String getSimpleTypeName(Dependency dependency) {
        String typeName = dependency.typeMirror().toString();
        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            return typeName
                    .substring(nameIndex + 1);
        }
        return typeName;
    }

    private static String clean(String dirtyString) {
        return dirtyString
                .replaceAll("[^A-Za-z0-9]", "");
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
                String endName = clean(typeName.substring(nameIndex + 1));
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

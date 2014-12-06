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
import java.util.Iterator;
import java.util.List;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * @author rees.byars
 */
class ProviderWriter {

    private final ProviderNames providerNames;
    private final Filer filer;

    ProviderWriter(
            ProviderNames providerNames,
            Filer filer) {
        this.providerNames = providerNames;
        this.filer = filer;
    }

    void writeProviderFor(Binding binding) throws IOException {

        Dependency fulfilledDependency = binding.fulfilledDependency();
        String typeName = providerNames.getProviderNameFor(fulfilledDependency);

        JavaFileObject javaFileObject = filer.createSourceFile(typeName);
        JavaWriter javaWriter = new JavaWriter(javaFileObject.openWriter());

        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            javaWriter.emitPackage(typeName.substring(0, nameIndex));
        }

        List<Dependency> requiredDependencies = binding.requiredDependencies();
        List<Dependency> requiredDependenciesPlusProviderInstance =
                new ArrayList<>(requiredDependencies);
        Dependency providerInstanceDependency = binding.providerInstanceDependency();
        if (providerInstanceDependency != null) {
            requiredDependenciesPlusProviderInstance.add(providerInstanceDependency);
        }
        writeImportsForRequiredDependencies(javaWriter, requiredDependencies);
        // TODO write qualifier annotation?
        writeTypeDeclaration(javaWriter, typeName, fulfilledDependency);
        writeFieldsForRequiredDependencies(javaWriter, requiredDependenciesPlusProviderInstance);
        writeConstructorForRequiredDependencies(javaWriter, requiredDependenciesPlusProviderInstance);

        writeProvisionMethod(
                javaWriter, fulfilledDependency, requiredDependencies, binding);
        javaWriter.endType().close();

    }

    void writeImportsForRequiredDependencies(
            JavaWriter javaWriter,
            List<Dependency> dependencies) throws IOException {
        List<String> imports = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            // TODO will break on generic types
            imports.add(providerNames.getProviderNameFor(dependency));
        }
        imports.add(Provider.class.getName());
        javaWriter.emitImports(imports);
        javaWriter.emitEmptyLine();
    }

    void writeTypeDeclaration(
            JavaWriter javaWriter, String typeName, Dependency fulfilledDependency) throws IOException {
        javaWriter.beginType(
                typeName,
                "class",
                EnumSet.of(PUBLIC, FINAL),
                null,
                "Provider<" + fulfilledDependency.typeMirror().toString() + ">");
        javaWriter.emitEmptyLine();
    }

    void writeFieldsForRequiredDependencies(
            JavaWriter javaWriter,
            List<Dependency> dependencies) throws IOException {
        if (!dependencies.isEmpty()) {
            for (Dependency dependency : dependencies) {
                javaWriter.emitField(
                        providerNames.getProviderNameFor(dependency),
                        getFieldNameForDependency(dependency),
                        EnumSet.of(PRIVATE, FINAL));
            }
            javaWriter.emitEmptyLine();
        }
    }

    void writeConstructorForRequiredDependencies(
            JavaWriter javaWriter,
            List<Dependency> dependencies) throws IOException {
        List<String> parameters = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            parameters.add(providerNames.getProviderNameFor(dependency));
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
        javaWriter.emitEmptyLine();
    }

    void writeProvisionMethod(
            JavaWriter javaWriter,
            Dependency fulfilledDependency,
            List<Dependency> dependencies,
            Binding binding) throws IOException {

        javaWriter
                .beginMethod(
                        fulfilledDependency.typeMirror().toString(),
                        "get",
                        EnumSet.of(PUBLIC));

        if (ProviderKind.STATIC_CONSTRUCTOR == binding.kind()
                || ProviderKind.INSTANCE_CONSTRUCTOR == binding.kind()) {
            StringBuilder builder = new StringBuilder();
            builder.append("return ");
            if (binding.providerInstanceDependency() != null) {
                builder.append(getFieldNameForDependency(binding.providerInstanceDependency()))
                        .append(".get()");
            }
            builder.append("new ")
                    .append(binding.location().metadata().element().getSimpleName().toString())
                    .append("(");
            writeCommaSeparatedGetsFor(dependencies, builder);
            javaWriter.emitStatement(builder.append(")").toString());
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("return ");
            if (binding.providerInstanceDependency() != null) {
                builder.append(getFieldNameForDependency(binding.providerInstanceDependency()))
                        .append(".get()");
            } else {
                builder.append(binding.location().metadata().element().getSimpleName().toString());
            }
            builder.append(".")
                    .append(binding.providerMetadata().element().getSimpleName().toString())
                    .append("(");
            writeCommaSeparatedGetsFor(dependencies, builder);
            javaWriter.emitStatement(builder.append(")").toString());
        }
        javaWriter.endMethod();
        javaWriter.emitEmptyLine();
    }

    private void writeCommaSeparatedGetsFor(List<Dependency> requiredDependencies, StringBuilder builder) {
        for (Iterator<Dependency> i = requiredDependencies.iterator();
             i.hasNext();) {
            builder
                    .append(getFieldNameForDependency(i.next()))
                    .append(".get()");
            if (i.hasNext()) {
                builder.append(", ");
            }
        }
    }


    // TODO the names should use caching strategy similar to the ProviderNames but per-binding

    private String getFieldNameForDependency(Dependency dependency) {
        return Introspector.decapitalize(getSimpleTypeName(dependency));
    }

    private String getSimpleTypeName(Dependency dependency) {
        String typeName = providerNames.getProviderNameFor(dependency);
        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            return typeName
                    .substring(nameIndex + 1);
        }
        return typeName;
    }

}

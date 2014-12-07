package io.gunmetal.compiler;

import com.squareup.javawriter.JavaWriter;
import io.gunmetal.Provider;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * @author rees.byars
 */
class ProviderWriter {

    private final Map<Dependency, WritableProvider> writableProviders;
    private final Filer filer;

    ProviderWriter(
            Map<Dependency, WritableProvider> writableProviders,
            Filer filer) {
        this.writableProviders = writableProviders;
        this.filer = filer;
    }

    void writeProviderFor(Binding binding) throws IOException {

        Dependency fulfilledDependency = binding.fulfilledDependency();
        WritableProvider writableProvider = writableProviders.get(fulfilledDependency);

        JavaFileObject javaFileObject = filer.createSourceFile(writableProvider.typeName());
        JavaWriter javaWriter = new JavaWriter(javaFileObject.openWriter());

        javaWriter.emitPackage(writableProvider.packageName());

        List<WritableProvider> requiredDependencies = toWritableProviders(binding.requiredDependencies());
        List<WritableProvider> requiredDependenciesPlusProviderInstance =
                new ArrayList<>(requiredDependencies);
        Dependency providerInstanceDependency = binding.providerInstanceDependency();
        if (providerInstanceDependency != null) {
            requiredDependenciesPlusProviderInstance.add(writableProviders.get(providerInstanceDependency));
        }
        writeImportsForRequiredDependencies(javaWriter, requiredDependencies);
        // TODO write qualifier annotation?
        writeTypeDeclaration(javaWriter, writableProvider.typeName(), fulfilledDependency);
        writeFieldsForRequiredDependencies(javaWriter, requiredDependenciesPlusProviderInstance);
        writeConstructorForRequiredDependencies(javaWriter, requiredDependenciesPlusProviderInstance);

        writeProvisionMethod(
                javaWriter, fulfilledDependency, requiredDependencies, binding);
        javaWriter.endType().close();

    }

    List<WritableProvider> toWritableProviders(List<Dependency> dependencies) {
        List<WritableProvider> writableProviderList = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            writableProviderList.add(writableProviders.get(dependency));
        }
        return writableProviderList;
    }

    void writeImportsForRequiredDependencies(
            JavaWriter javaWriter,
            List<WritableProvider> dependencies) throws IOException {
        List<String> imports = new ArrayList<>();
        for (WritableProvider dependency : dependencies) {
            imports.add(dependency.typeName());
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
            List<WritableProvider> dependencies) throws IOException {
        if (!dependencies.isEmpty()) {
            for (WritableProvider dependency : dependencies) {
                javaWriter.emitField(
                        dependency.simpleTypeName(),
                        dependency.fieldName(),
                        EnumSet.of(PRIVATE, FINAL));
            }
            javaWriter.emitEmptyLine();
        }
    }

    void writeConstructorForRequiredDependencies(
            JavaWriter javaWriter,
            List<WritableProvider> dependencies) throws IOException {
        List<String> parameters = new ArrayList<>();
        for (WritableProvider dependency : dependencies) {
            parameters.add(dependency.simpleTypeName());
            parameters.add(dependency.fieldName());
        }
        javaWriter
                .beginConstructor(EnumSet.of(PUBLIC), parameters, Collections.<String>emptyList());
        for (WritableProvider dependency : dependencies) {
            String statement =
                    "this." + dependency.fieldName()
                    + " = " + dependency.fieldName();
            javaWriter.emitStatement(statement);
        }
        javaWriter.endConstructor();
        javaWriter.emitEmptyLine();
    }

    void writeProvisionMethod(
            JavaWriter javaWriter,
            Dependency fulfilledDependency,
            List<WritableProvider> dependencies,
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
                builder.append(writableProviders.get(binding.providerInstanceDependency()).fieldName())
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
                builder.append(writableProviders.get(binding.providerInstanceDependency()).fieldName())
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

    private void writeCommaSeparatedGetsFor(List<WritableProvider> requiredDependencies, StringBuilder builder) {
        for (Iterator<WritableProvider> i = requiredDependencies.iterator();
             i.hasNext();) {
            builder
                    .append(i.next().fieldName())
                    .append(".get()");
            if (i.hasNext()) {
                builder.append(", ");
            }
        }
    }

}

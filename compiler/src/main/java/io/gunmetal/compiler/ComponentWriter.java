package io.gunmetal.compiler;

import com.squareup.javawriter.JavaWriter;
import io.gunmetal.Provider;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * @author rees.byars
 */
public class ComponentWriter {

    private final Map<Dependency, WritableProvider> writableProviders;
    private final Filer filer;

    ComponentWriter(
            Map<Dependency, WritableProvider> writableProviders,
            Filer filer) {
        this.writableProviders = writableProviders;
        this.filer = filer;
    }

    void writeBigUglyBlob(Queue<Binding> bindings) throws IOException {

        // TODO, obviously just working on building the providers at this point...
        String componentTypeName = "bluh.BigUglyBlob";
        String componentPackageName = "bluh";

        JavaFileObject javaFileObject = filer.createSourceFile(componentTypeName);
        JavaWriter javaWriter = new JavaWriter(javaFileObject.openWriter());
        javaWriter.emitPackage(componentPackageName);

        List<WritableProvider> writableProviders = toWritableProviders(bindings);
        writeImportsForRequiredDependencies(javaWriter, writableProviders);
        writeTypeDeclaration(javaWriter, componentTypeName);

        writeFieldsForRequiredDependencies(javaWriter, writableProviders);
        writeConstructor(javaWriter, bindings);

        javaWriter.endType().close();

    }

    List<WritableProvider> toWritableProviders(Queue<Binding> bindings) {
        List<WritableProvider> writableProviderList = new ArrayList<>();
        for (Binding binding : bindings) {
            writableProviderList.add(writableProviders.get(binding.fulfilledDependency()));
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
            JavaWriter javaWriter, String typeName) throws IOException {
        javaWriter.beginType(
                typeName,
                "class",
                EnumSet.of(PUBLIC, FINAL));
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

    void writeConstructor(
            JavaWriter javaWriter,
            Queue<Binding> bindings) throws IOException {
        javaWriter.beginConstructor(EnumSet.of(PUBLIC));
        writeStatements(javaWriter, bindings);
        javaWriter.endConstructor();
        javaWriter.emitEmptyLine();
    }

    void writeStatements(
            JavaWriter javaWriter,
            Queue<Binding> bindings) throws IOException {
        for (; !bindings.isEmpty();) {
            Binding binding = bindings.poll();
            Dependency fulfilledDependency = binding.fulfilledDependency();
            WritableProvider writableProvider = writableProviders.get(fulfilledDependency);
            StringBuilder statementBuilder = new StringBuilder();
            statementBuilder
                    .append(writableProvider.fieldName())
                    .append(" = new ")
                    .append(writableProvider.simpleTypeName())
                    .append("(");
            List<Dependency> requiredDependencies = binding.requiredDependencies();
            List<Dependency> requiredDependenciesPlusProviderInstance =
                    new ArrayList<>(requiredDependencies);
            Dependency providerInstanceDependency = binding.providerInstanceDependency();
            if (providerInstanceDependency != null) {
                requiredDependenciesPlusProviderInstance.add(providerInstanceDependency);
            }
            writeCommaSeparated(requiredDependenciesPlusProviderInstance, statementBuilder);
            statementBuilder.append(")");
            javaWriter.emitStatement(statementBuilder.toString());
        }
    }

    private void writeCommaSeparated(List<Dependency> requiredDependencies, StringBuilder builder) {
        for (Iterator<Dependency> i = requiredDependencies.iterator();
             i.hasNext();) {
            builder
                    .append(writableProviders.get(i.next()).fieldName());
            if (i.hasNext()) {
                builder.append(", ");
            }
        }
    }

}

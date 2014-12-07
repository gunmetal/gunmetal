package io.gunmetal.compiler;

import java.beans.Introspector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
class WritableProviderRepository {

    private final Set<String> simpleTypeNames = new HashSet<>();
    private final Map<Dependency, WritableProvider> writableProviders = new HashMap<>();

    WritableProvider addFor(Binding binding) {

        Dependency dependency = binding.fulfilledDependency();
        WritableProvider writableProvider = writableProviders.get(dependency);
        if (writableProvider != null) {
            return writableProvider;
        }

        // TODO for generics this is not ideal...
        String typeName = dependency.typeMirror().toString();
        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            // Strip special chars for generics, etc
            typeName = clean(typeName.substring(nameIndex + 1));
        }

        String packageName = packageForBinding(binding);
        String simpleTypeName = typeName + "_Provider";

        return resolveFor(dependency, packageName , simpleTypeName, 0);
    }

    Map<Dependency, WritableProvider> asMap() {
        return writableProviders;
    }

    private WritableProvider resolveFor(
            Dependency dependency,
            String packageName,
            String simpleTypeName,
            int index) {
        if (index != 0) {
            simpleTypeName += index;
        }
        if (simpleTypeNames.contains(simpleTypeName)) {
            return resolveFor(dependency, packageName, simpleTypeName, index + 1);
        }
        simpleTypeNames.add(simpleTypeName);
        WritableProvider writableProvider =
                new WritableProvider(
                        packageName,
                        packageName + "." + simpleTypeName,
                        simpleTypeName,
                        Introspector.decapitalize(simpleTypeName));
        writableProviders.put(dependency, writableProvider);
        return writableProvider;
    }

    private static String packageForBinding(Binding binding) {
        String className = binding.location().metadata().element().asType().toString();
        int packageNameIndex = className.lastIndexOf(".");
        if (packageNameIndex > 0) {
            return className.substring(0, packageNameIndex);
        }
        return "";
    }

    private static String clean(String dirtyString) {
        return dirtyString
                .replaceAll("[^A-Za-z0-9]", "");
    }

}

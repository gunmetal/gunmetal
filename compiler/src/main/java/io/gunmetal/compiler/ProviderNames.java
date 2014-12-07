package io.gunmetal.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
final class ProviderNames {

    // TODO create class such as WritableProvider with methods importString, fieldName, simpleTypeName, etc

    private final Map<Dependency, String> providerNames = new HashMap<>();

    String getProviderNameFor(Binding binding) {
        Dependency dependency = binding.fulfilledDependency();
        String name = providerNames.get(dependency);
        if (name != null) {
            return name;
        }

        // TODO for generics this is not ideal...
        String typeName = dependency.typeMirror().toString();
        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            // Strip special chars for generics, etc
            typeName = clean(typeName.substring(nameIndex + 1));
        }

        return getProviderNameFor(dependency, packageForBinding(binding) + "." + typeName + "_Provider", 0);
    }

    String getProviderNameFor(Dependency dependency) {
        return providerNames.get(dependency);
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

    private String packageForBinding(Binding binding) {
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

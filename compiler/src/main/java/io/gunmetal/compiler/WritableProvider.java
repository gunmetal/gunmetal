package io.gunmetal.compiler;

/**
 * @author rees.byars
 */
final class WritableProvider {

    private final String packageName;
    private final String typeName;
    private final String simpleTypeName;
    private final String fieldName;

    WritableProvider(
            String packageName,
            String typeName,
            String simpleTypeName,
            String fieldName) {
        this.packageName = packageName;
        this.typeName = typeName;
        this.simpleTypeName = simpleTypeName;
        this.fieldName = fieldName;
    }

    String packageName() {
        return packageName;
    }

    String typeName() {
        return typeName;
    }

    String simpleTypeName() {
        return simpleTypeName;
    }

    String fieldName() {
        return fieldName;
    }

}

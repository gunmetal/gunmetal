package io.gunmetal.builder;

/**
 * @author rees.byars
 */
interface AccessRestrictedComponentAdapter<T> extends AccessFilter<AccessRestrictedComponentAdapter>, ComponentAdapter<T> {

    // TODO instead of making visibility adapter accessible, add Dependency to ResolutionContext,
    // TODO and add requesting ComponentAdapter to Dependency?

    ModuleAdapter getModuleAdapter();

    interface ModuleAdapter extends AccessFilter<AccessRestrictedComponentAdapter<?>> {

        Class<?> getModuleClass();

        CompositeQualifier getCompositeQualifier();

    }

}

package io.gunmetal.builder;

import io.gunmetal.AccessRestrictions;
import io.gunmetal.Module;

import java.util.List;

/**
 * @author rees.byars
 */
public class ReflectiveModuleBuilder implements ModuleBuilder {

    private final ComponentAdapterFactory componentAdapterFactory;
    private final MetadataAdapter metadataAdapter;

    public ReflectiveModuleBuilder(ComponentAdapterFactory componentAdapterFactory, MetadataAdapter metadataAdapter) {
        this.componentAdapterFactory = componentAdapterFactory;
        this.metadataAdapter = metadataAdapter;
    }

    @Override
    public ModuleAdapter build(final Class<?> moduleClass, InternalProvider internalProvider) {

        Module moduleAnnotation = moduleClass.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + moduleClass.getName() + "] must be annotated with @Module()");
        }

        final VisibilityAdapter<ComponentAdapter<?>> blackListAdapter = getBlackListAdapter(moduleAnnotation);

        final VisibilityAdapter<ComponentAdapter<?>> whiteListAdapter = getWhiteListAdapter(moduleAnnotation);

        final VisibilityAdapter<Class<?>> moduleClassVisibilityAdapter =
                VisibilityAdapter.Factory.getVisibilityAdapter(moduleClass);

        return new ModuleAdapter() {

            final Object[] qualifiers = ReflectionUtils.getQualifiers(moduleClass, metadataAdapter.getQualifierAnnotation());

            @Override
            public Class<?> getModuleClass() {
                return moduleClass;
            }

            @Override
            public List<ComponentAdapter> getComponentAdapters() {
                // TODO
                return null;
            }

            @Override
            public Object[] getQualifiers() {
                return qualifiers;
            }

            @Override
            public boolean isVisibleTo(ComponentAdapter<?> target) {
                return moduleClassVisibilityAdapter.isVisibleTo(target.getModuleAdapter().getModuleClass()) &&
                        moduleClassVisibilityAdapter.isVisibleTo(target.getComponentClass()) &&
                        blackListAdapter.isVisibleTo(target) &&
                        whiteListAdapter.isVisibleTo(target);
            }
        };
        
    }
    
    private VisibilityAdapter<ComponentAdapter<?>> getBlackListAdapter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.NotAccessibleFrom> blackListClass = moduleAnnotation.notAccessibleFrom();

        if (blackListClass == AccessRestrictions.NotAccessibleFrom.class) {

            return new VisibilityAdapter<ComponentAdapter<?>>() {
                @Override
                public boolean isVisibleTo(ComponentAdapter<?> target) {
                    return true;
                }
            };
            
        }

        final Class[] blackListClasses;

        AccessRestrictions.Modules blackListModules = blackListClass.getAnnotation(AccessRestrictions.Modules.class);

        if (blackListModules != null) {

            blackListClasses = blackListModules.value();

        } else {

            blackListClasses = new Class[]{};

        }

        final Object[] blackListQualifiers = ReflectionUtils.getQualifiers(blackListClass, metadataAdapter.getQualifierAnnotation());

        return  new VisibilityAdapter<ComponentAdapter<?>>() {
            @Override
            public boolean isVisibleTo(ComponentAdapter<?> target) {
                for (Class<?> blackListClass : blackListClasses) {
                    if (blackListClass == target.getModuleAdapter().getModuleClass()) {
                        return false;
                    }
                }
                for (Object targetQualifier : target.getQualifiers()) {
                    for (Object blackListQualifier : blackListQualifiers) {
                        if (targetQualifier.equals(blackListQualifier)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
        
    }

    private VisibilityAdapter<ComponentAdapter<?>> getWhiteListAdapter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.OnlyAccessibleFrom> whiteListClass = moduleAnnotation.onlyAccessibleFrom();

        if (whiteListClass == AccessRestrictions.OnlyAccessibleFrom.class) {

            return new VisibilityAdapter<ComponentAdapter<?>>() {
                @Override
                public boolean isVisibleTo(ComponentAdapter<?> target) {
                    return true;
                }
            };

        }

        final Class[] whiteListClasses;

        AccessRestrictions.Modules whiteListModules = whiteListClass.getAnnotation(AccessRestrictions.Modules.class);

        if (whiteListModules != null) {

            whiteListClasses = whiteListModules.value();

        } else {

            whiteListClasses = new Class[]{};

        }

        final Object[] whiteListQualifiers = ReflectionUtils.getQualifiers(whiteListClass, metadataAdapter.getQualifierAnnotation());

        return  new VisibilityAdapter<ComponentAdapter<?>>() {
            @Override
            public boolean isVisibleTo(ComponentAdapter<?> target) {
                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == target.getModuleAdapter().getModuleClass()) {
                        return true;
                    }
                }
                for (Object targetQualifier : target.getQualifiers()) {
                    for (Object whiteListQualifier : whiteListQualifiers) {
                        if (targetQualifier.equals(whiteListQualifier)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };

    }

}

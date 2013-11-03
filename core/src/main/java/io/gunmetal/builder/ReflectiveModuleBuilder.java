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

        final AccessFilter<ComponentAdapter<?>> blackListFilter = getBlackListFilter(moduleAnnotation);

        final AccessFilter<ComponentAdapter<?>> whiteListFilter = getWhiteListFilter(moduleAnnotation);

        final AccessFilter<Class<?>> moduleClassAccessFilter =
                AccessFilter.Factory.getAccessFilter(moduleClass);

        Object[] qualifiers = ReflectionUtils.getQualifiers(moduleClass, metadataAdapter.getQualifierAnnotation());

        final CompositeQualifier compositeQualifier = CompositeQualifier.Factory.create(qualifiers);

        return new ModuleAdapter() {

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
            public CompositeQualifier getCompositeQualifier() {
                return compositeQualifier;
            }

            @Override
            public boolean isAccessibleFrom(ComponentAdapter<?> target) {
                return moduleClassAccessFilter.isAccessibleFrom(target.getModuleAdapter().getModuleClass()) &&
                        moduleClassAccessFilter.isAccessibleFrom(target.getComponentClass()) &&
                        blackListFilter.isAccessibleFrom(target) &&
                        whiteListFilter.isAccessibleFrom(target);
            }
        };
        
    }
    
    private AccessFilter<ComponentAdapter<?>> getBlackListFilter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.NotAccessibleFrom> blackListClass = moduleAnnotation.notAccessibleFrom();

        if (blackListClass == AccessRestrictions.NotAccessibleFrom.class) {

            return new AccessFilter<ComponentAdapter<?>>() {
                @Override
                public boolean isAccessibleFrom(ComponentAdapter<?> target) {
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

        return  new AccessFilter<ComponentAdapter<?>>() {
            @Override
            public boolean isAccessibleFrom(ComponentAdapter<?> target) {
                for (Class<?> blackListClass : blackListClasses) {
                    if (blackListClass == target.getModuleAdapter().getModuleClass()) {
                        return false;
                    }
                }
                return !target.getCompositeQualifier().intersects(blackListQualifiers);
            }
        };
        
    }

    private AccessFilter<ComponentAdapter<?>> getWhiteListFilter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.OnlyAccessibleFrom> whiteListClass = moduleAnnotation.onlyAccessibleFrom();

        if (whiteListClass == AccessRestrictions.OnlyAccessibleFrom.class) {

            return new AccessFilter<ComponentAdapter<?>>() {
                @Override
                public boolean isAccessibleFrom(ComponentAdapter<?> target) {
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

        return  new AccessFilter<ComponentAdapter<?>>() {
            @Override
            public boolean isAccessibleFrom(ComponentAdapter<?> target) {
                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == target.getModuleAdapter().getModuleClass()) {
                        return true;
                    }
                }
                return target.getCompositeQualifier().intersects(whiteListQualifiers);
            }
        };

    }

}

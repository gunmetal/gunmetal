package io.gunmetal.builder;

import io.gunmetal.AccessLevel;
import io.gunmetal.AccessRestrictions;
import io.gunmetal.Component;
import io.gunmetal.Module;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
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
    public List<ComponentAdapter<?>> build(final Class<?> moduleClass, InternalProvider internalProvider) {

        final Module moduleAnnotation = moduleClass.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + moduleClass.getName() 
                    + "] must be annotated with @Module()");
        }

        final AccessFilter<DependencyRequest> blackListFilter = getBlackListFilter(moduleClass, moduleAnnotation);

        final AccessFilter<DependencyRequest> whiteListFilter = getWhiteListFilter(moduleClass, moduleAnnotation);

        final AccessFilter<DependencyRequest> dependsOnFilter = getDependsOnFilter(moduleClass);

        AccessLevel moduleAccessLevel = moduleAnnotation.access();

        final AccessFilter<Class<?>> moduleClassAccessFilter =
                AccessFilter.Factory.getAccessFilter(moduleAccessLevel, moduleClass);

        Object[] qualifiers = ReflectionUtils.getQualifiers(moduleClass, metadataAdapter.getQualifierAnnotation());

        final CompositeQualifier compositeQualifier = CompositeQualifier.Factory.create(qualifiers);

        final ModuleAdapter moduleAdapter = new ModuleAdapter() {

            @Override
            public Class<?> getModuleClass() {
                return moduleClass;
            }

            @Override
            public CompositeQualifier getCompositeQualifier() {
                return compositeQualifier;
            }

            @Override
            public Class<?>[] getReferencedModules() {
                return moduleAnnotation.dependsOn();
            }

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                ModuleAdapter requestSourceModule = dependencyRequest.getRequestSource().getModuleAdapter();
                // we the single & because we want to process them all regardless if one fails
                return moduleClassAccessFilter.isAccessibleTo(requestSourceModule.getModuleClass()) 
                        & dependsOnFilter.isAccessibleTo(dependencyRequest)
                        & blackListFilter.isAccessibleTo(dependencyRequest)
                        & whiteListFilter.isAccessibleTo(dependencyRequest);
            }
        };

        List<ComponentAdapter<?>> componentAdapters = new LinkedList<ComponentAdapter<?>>();

        for (Component component : moduleAnnotation.components()) {

            final AccessFilter<Class<?>> accessFilter = 
                    AccessFilter.Factory.getAccessFilter(component.access(), component.type());

            componentAdapters.add(componentAdapterFactory.create(component, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest) 
                            && accessFilter.isAccessibleTo(dependencyRequest.getRequestSource().getComponentClass());
                }
            }, internalProvider));

        }

        for (Method method : moduleClass.getMethods()) {

            int modifiers = method.getModifiers();

            if (!Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("A module's provider methods must be static.  The method ["
                        + method.getName() +"] in module [" + moduleClass.getName() + "] is not static.");
            }

            if (method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException("A module's provider methods must have a return type.  The method ["
                        + method.getName() +"] in module [" + moduleClass.getName() + "] has a void return type.");
            }

            final AccessFilter<Class<?>> accessFilter = AccessFilter.Factory.getAccessFilter(method);

            componentAdapters.add(componentAdapterFactory.create(method, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest) 
                            && accessFilter.isAccessibleTo(dependencyRequest.getRequestSource().getComponentClass());
                }
            }, internalProvider));

        }

        return componentAdapters;
        
    }

    private AccessFilter<DependencyRequest> getBlackListFilter(final Class<?> moduleClass, Module moduleAnnotation) {

        Class<? extends AccessRestrictions.NotAccessibleFrom> blackListConfigClass = moduleAnnotation.notAccessibleFrom();

        if (blackListConfigClass == AccessRestrictions.NotAccessibleFrom.class) {

            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return true;
                }
            };
            
        }

        final Class[] blackListClasses;

        AccessRestrictions.Modules blackListModules = blackListConfigClass.getAnnotation(AccessRestrictions.Modules.class);

        if (blackListModules != null) {

            blackListClasses = blackListModules.value();

        } else {

            blackListClasses = new Class[]{};

        }

        final Object[] blackListQualifiers = 
                ReflectionUtils.getQualifiers(blackListConfigClass, metadataAdapter.getQualifierAnnotation());

        return  new AccessFilter<DependencyRequest>() {

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {

                ComponentAdapter<?> requestSource = dependencyRequest.getRequestSource();
                Class<?> requestingSourceModuleClass = requestSource.getModuleAdapter().getModuleClass();

                for (Class<?> blackListClass : blackListClasses) {

                    if (blackListClass == requestingSourceModuleClass) {

                        dependencyRequest.addError("The module [" + requestingSourceModuleClass.getName() +
                                "] does not have access to the module [" + moduleClass.getName() + "].");

                        return false;

                    }

                }

                boolean qualifierMatch = requestSource.getCompositeQualifier().intersects(blackListQualifiers);

                if (qualifierMatch) {
                    dependencyRequest.addError("The module [" + requestingSourceModuleClass.getName() +
                            "] does not have access to the module [" + moduleClass.getName() + "].");
                }

                return !qualifierMatch;

            }

        };
        
    }

    private AccessFilter<DependencyRequest> getWhiteListFilter(final Class<?> moduleClass, Module moduleAnnotation) {

        Class<? extends AccessRestrictions.OnlyAccessibleFrom> whiteListConfigClass = moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClass == AccessRestrictions.OnlyAccessibleFrom.class) {

            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return true;
                }
            };

        }

        final Class[] whiteListClasses;

        AccessRestrictions.Modules whiteListModules = whiteListConfigClass.getAnnotation(AccessRestrictions.Modules.class);

        if (whiteListModules != null) {

            whiteListClasses = whiteListModules.value();

        } else {

            whiteListClasses = new Class[]{};

        }

        final Object[] whiteListQualifiers = ReflectionUtils.getQualifiers(whiteListConfigClass, metadataAdapter.getQualifierAnnotation());

        return  new AccessFilter<DependencyRequest>() {

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {

                ComponentAdapter<?> requestSource = dependencyRequest.getRequestSource();
                Class<?> requestingSourceModuleClass = requestSource.getModuleAdapter().getModuleClass();

                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == requestingSourceModuleClass) {
                        return true;
                    }
                }

                boolean qualifierMatch = requestSource.getCompositeQualifier().intersects(whiteListQualifiers);

                if (!qualifierMatch) {

                    dependencyRequest.addError("The module [" + requestingSourceModuleClass.getName() +
                            "] does not have access to the module [" + moduleClass.getName() + "].");

                }

                return qualifierMatch;

            }

        };

    }

    private AccessFilter<DependencyRequest> getDependsOnFilter(final Class<?> moduleClass) {

        return new AccessFilter<DependencyRequest>() {

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {

                ModuleAdapter requestSourceModule = dependencyRequest.getRequestSource().getModuleAdapter();

                for (Class<?> dependency : requestSourceModule.getReferencedModules()) {
                    if (moduleClass == dependency) {
                        return true;
                    }
                }

                dependencyRequest.addError("The module [" + requestSourceModule.getModuleClass().getName() +
                        "] does not have access to the module [" + moduleClass.getName() + "].");

                return false;

            }

        };

    }

}

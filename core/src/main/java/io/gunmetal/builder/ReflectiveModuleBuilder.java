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

        Module moduleAnnotation = moduleClass.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + moduleClass.getName() 
                    + "] must be annotated with @Module()");
        }

        final AccessFilter<DependencyRequest> blackListFilter = getBlackListFilter(moduleAnnotation);

        final AccessFilter<DependencyRequest> whiteListFilter = getWhiteListFilter(moduleAnnotation);

        final AccessFilter<Class<?>> dependsOnFilter = getDependsOnFilter(moduleClass, moduleAnnotation);

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
            public boolean dependsOn(Class<?> otherModule) {
                return dependsOnFilter.isAccessibleTo(otherModule);
            }

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                ModuleAdapter requestSourceModule = dependencyRequest.getRequestSource().getModuleAdapter();
                return moduleClassAccessFilter.isAccessibleTo(requestSourceModule.getModuleClass()) 
                        && requestSourceModule.dependsOn(moduleClass) 
                        && blackListFilter.isAccessibleTo(dependencyRequest) 
                        && whiteListFilter.isAccessibleTo(dependencyRequest);
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

    private AccessFilter<DependencyRequest> getBlackListFilter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.NotAccessibleFrom> blackListClass = moduleAnnotation.notAccessibleFrom();

        if (blackListClass == AccessRestrictions.NotAccessibleFrom.class) {

            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
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

        final Object[] blackListQualifiers = 
                ReflectionUtils.getQualifiers(blackListClass, metadataAdapter.getQualifierAnnotation());

        return  new AccessFilter<DependencyRequest>() {
            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                ComponentAdapter<?> requestSource = dependencyRequest.getRequestSource();
                Class<?> requestingSourceModuleClass = requestSource.getModuleAdapter().getModuleClass();
                for (Class<?> blackListClass : blackListClasses) {
                    if (blackListClass == requestingSourceModuleClass) {
                        return false;
                    }
                }
                return !requestSource.getCompositeQualifier().intersects(blackListQualifiers);
            }
        };
        
    }

    private AccessFilter<DependencyRequest> getWhiteListFilter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.OnlyAccessibleFrom> whiteListClass = moduleAnnotation.onlyAccessibleFrom();

        if (whiteListClass == AccessRestrictions.OnlyAccessibleFrom.class) {

            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
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
                return requestSource.getCompositeQualifier().intersects(whiteListQualifiers);
            }
        };

    }

    private AccessFilter<Class<?>> getDependsOnFilter(final Class<?> moduleClass, Module moduleAnnotation) {

        final Class<?>[] dependencies = moduleAnnotation.dependsOn();

        if (dependencies.length == 0) {

            return new AccessFilter<Class<?>>() {
                @Override
                public boolean isAccessibleTo(Class<?> targetModuleClass) {
                    return true;
                }
            };

        }

        return new AccessFilter<Class<?>>() {

            @Override
            public boolean isAccessibleTo(Class<?> targetModuleClass) {
                for (Class<?> dependency : dependencies) {
                    if (targetModuleClass == dependency) {
                        return true;
                    }
                }
                throw new IllegalAccessError("The module [" + moduleClass.getName() +
                        "] does not have access to the module [" + targetModuleClass.getName() + "].");
            }

        };

    }

}

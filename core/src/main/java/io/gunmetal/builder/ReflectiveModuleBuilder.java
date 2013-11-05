package io.gunmetal.builder;

import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.scope.Scope;
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
    public List<AccessRestrictedComponentAdapter<?>> build(final Class<?> moduleClass, InternalProvider internalProvider) {

        Module moduleAnnotation = moduleClass.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + moduleClass.getName() + "] must be annotated with @Module()");
        }

        final AccessFilter<AccessRestrictedComponentAdapter<?>> blackListFilter = getBlackListFilter(moduleAnnotation);

        final AccessFilter<AccessRestrictedComponentAdapter<?>> whiteListFilter = getWhiteListFilter(moduleAnnotation);

        final AccessFilter<Class<?>> dependsOnFilter = getDependsOnFilter(moduleAnnotation);

        AccessLevel moduleAccessLevel = moduleAnnotation.access();

        final AccessFilter<Class<?>> moduleClassAccessFilter =
                AccessFilter.Factory.getAccessFilter(moduleAccessLevel, moduleClass);

        Object[] qualifiers = ReflectionUtils.getQualifiers(moduleClass, metadataAdapter.getQualifierAnnotation());

        final CompositeQualifier compositeQualifier = CompositeQualifier.Factory.create(qualifiers);

        AccessRestrictedComponentAdapter.ModuleAdapter moduleAdapter = new AccessRestrictedComponentAdapter.ModuleAdapter() {

            @Override
            public Class<?> getModuleClass() {
                return moduleClass;
            }

            @Override
            public CompositeQualifier getCompositeQualifier() {
                return compositeQualifier;
            }

            @Override
            public boolean isAccessibleFrom(AccessRestrictedComponentAdapter<?> target) {
                return moduleClassAccessFilter.isAccessibleFrom(target.getModuleAdapter().getModuleClass()) &&
                        // TODO moduleClassAccessFilter.isAccessibleFrom(target.getComponentClass()) &&
                        dependsOnFilter.isAccessibleFrom(target.getModuleAdapter().getModuleClass()) &&
                        blackListFilter.isAccessibleFrom(target) &&
                        whiteListFilter.isAccessibleFrom(target);
            }
        };

        List<AccessRestrictedComponentAdapter<?>> componentAdapters = new LinkedList<AccessRestrictedComponentAdapter<?>>();

        for (Component component : moduleAnnotation.components()) {

            ComponentAdapter<?> componentAdapter = componentAdapterFactory.create(component, internalProvider);

            AccessFilter<Class<?>> accessFilter = AccessFilter.Factory.getAccessFilter(component.access(), component.type());

            componentAdapters.add(decorate(componentAdapter, moduleAdapter, accessFilter));

        }

        for (Method method : moduleClass.getMethods()) {

            int modifiers = method.getModifiers();

            // TODO exception messages

            if (!Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("TODO");
            }

            if (method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException("TODO");
            }

            ComponentAdapter<?> componentAdapter = componentAdapterFactory.create(method, internalProvider);

            AccessFilter<Class<?>> accessFilter = AccessFilter.Factory.getAccessFilter(method);

            componentAdapters.add(decorate(componentAdapter, moduleAdapter, accessFilter));

        }

        return componentAdapters;
        
    }

    private AccessFilter<AccessRestrictedComponentAdapter<?>> getBlackListFilter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.NotAccessibleFrom> blackListClass = moduleAnnotation.notAccessibleFrom();

        if (blackListClass == AccessRestrictions.NotAccessibleFrom.class) {

            return new AccessFilter<AccessRestrictedComponentAdapter<?>>() {
                @Override
                public boolean isAccessibleFrom(AccessRestrictedComponentAdapter<?> target) {
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

        return  new AccessFilter<AccessRestrictedComponentAdapter<?>>() {
            @Override
            public boolean isAccessibleFrom(AccessRestrictedComponentAdapter<?> target) {
                for (Class<?> blackListClass : blackListClasses) {
                    if (blackListClass == target.getModuleAdapter().getModuleClass()) {
                        return false;
                    }
                }
                return !target.getCompositeQualifier().intersects(blackListQualifiers);
            }
        };
        
    }

    private AccessFilter<AccessRestrictedComponentAdapter<?>> getWhiteListFilter(Module moduleAnnotation) {

        Class<? extends AccessRestrictions.OnlyAccessibleFrom> whiteListClass = moduleAnnotation.onlyAccessibleFrom();

        if (whiteListClass == AccessRestrictions.OnlyAccessibleFrom.class) {

            return new AccessFilter<AccessRestrictedComponentAdapter<?>>() {
                @Override
                public boolean isAccessibleFrom(AccessRestrictedComponentAdapter<?> target) {
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

        return  new AccessFilter<AccessRestrictedComponentAdapter<?>>() {
            @Override
            public boolean isAccessibleFrom(AccessRestrictedComponentAdapter<?> target) {
                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == target.getModuleAdapter().getModuleClass()) {
                        return true;
                    }
                }
                return target.getCompositeQualifier().intersects(whiteListQualifiers);
            }
        };

    }

    private AccessFilter<Class<?>> getDependsOnFilter(Module moduleAnnotation) {

        final Class<?>[] dependencies = moduleAnnotation.dependsOn();

        if (dependencies.length == 0) {

            return new AccessFilter<Class<?>>() {
                @Override
                public boolean isAccessibleFrom(Class<?> targetModuleClass) {
                    return true;
                }
            };

        }

        return new AccessFilter<Class<?>>() {

            @Override
            public boolean isAccessibleFrom(Class<?> targetModuleClass) {
                for (Class<?> dependency : dependencies) {
                    if (targetModuleClass == dependency) {
                        return true;
                    }
                }
                return false;
            }

        };

    }

    private <T> AccessRestrictedComponentAdapter<T> decorate(final ComponentAdapter<T> componentAdapter, final AccessRestrictedComponentAdapter.ModuleAdapter moduleAdapter, final AccessFilter<Class<?>> componentAccessFilter) {
        return new AccessRestrictedComponentAdapter<T>() {

            @Override
            public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return componentAdapter.get(internalProvider, resolutionContext);
            }

            @Override
            public ModuleAdapter getModuleAdapter() {
                return moduleAdapter;
            }

            @Override
            public boolean isAccessibleFrom(AccessRestrictedComponentAdapter target) {
                return moduleAdapter.isAccessibleFrom(target) && componentAccessFilter.isAccessibleFrom(target.getComponentClass());
            }

            @Override
            public Class<T> getComponentClass() {
                return componentAdapter.getComponentClass();
            }

            @Override
            public Scope getScope() {
                return componentAdapter.getScope();
            }

            @Override
            public CompositeQualifier getCompositeQualifier() {
                return componentAdapter.getCompositeQualifier();
            }
        };
    }

}

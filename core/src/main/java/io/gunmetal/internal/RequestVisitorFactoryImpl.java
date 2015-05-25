package io.gunmetal.internal;

import io.gunmetal.BlackList;
import io.gunmetal.Module;
import io.gunmetal.WhiteList;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.RequestVisitor;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;

/**
 * @author rees.byars
 */
class RequestVisitorFactoryImpl implements RequestVisitorFactory {

    private final QualifierResolver qualifierResolver;
    private final List<RequestVisitor> requestVisitors;
    private final boolean requireExplicitModuleDependencies;

    RequestVisitorFactoryImpl(QualifierResolver qualifierResolver,
                              List<RequestVisitor> requestVisitors,
                              boolean requireExplicitModuleDependencies) {
        this.qualifierResolver = qualifierResolver;
        this.requestVisitors = requestVisitors;
        this.requireExplicitModuleDependencies = requireExplicitModuleDependencies;
    }

    @Override public RequestVisitor resourceRequestVisitor(Resource resource,
                                                           ComponentContext context) {
        ResourceMetadata<?> resourceMetadata = resource.metadata();
        ModuleMetadata moduleMetadata = resourceMetadata.moduleMetadata();
        RequestVisitor moduleRequestVisitor = moduleRequestVisitor(moduleMetadata);
        RequestVisitor moduleResourceVisitor = moduleResourceVisitor(resourceMetadata, moduleMetadata);
        AccessFilter<Class<?>> resourceAccessFilter = accessFilter(resource);
        RequestVisitor resourceClassVisitor = (dependencyRequest, errors) -> {
            if (!resourceAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
                errors.add(
                        "The class [" + dependencyRequest.sourceOrigin().getName()
                                + "] does not have access to [" + resourceAccessFilter.filteredElement() + "]"
                );
            }
        };
        RequestVisitor scopeVisitor = (dependencyRequest, errors) -> {
            if (!dependencyRequest.sourceProvision().overrides().allowFuzzyScopes() &&
                    !resource.metadata().scope().canInject(dependencyRequest.sourceScope())) {
                errors.add("mis-scoped"); // TODO message
            }
        };
        return (dependencyRequest, errors) -> {
            moduleRequestVisitor.visit(dependencyRequest, errors);
            moduleResourceVisitor.visit(dependencyRequest, errors);
            resourceClassVisitor.visit(dependencyRequest, errors);
            scopeVisitor.visit(dependencyRequest, errors);
            for (RequestVisitor requestVisitor : requestVisitors) {
                requestVisitor.visit(dependencyRequest, errors);
            }
        };
    }

    private RequestVisitor moduleRequestVisitor(ModuleMetadata moduleMetadata) {
        Class<?> module = moduleMetadata.moduleClass();
        Module moduleAnnotation = moduleMetadata.moduleAnnotation();
        if (moduleAnnotation == Module.NONE) {
            return RequestVisitor.NONE;
        }
        RequestVisitor blackListVisitor = blackListVisitor(module, moduleAnnotation);
        RequestVisitor whiteListVisitor = whiteListVisitor(module, moduleAnnotation);
        RequestVisitor dependsOnVisitor = dependsOnVisitor(module);
        AccessFilter<Class<?>> moduleAccessFilter =
                AccessFilter.create(moduleAnnotation.access(), module);
        RequestVisitor moduleClassVisitor = (dependencyRequest, errors) -> {
            if (!moduleAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
                errors.add(
                        "The module [" + dependencyRequest.sourceModule().moduleClass().getName()
                                + "] does not have access to [" + moduleAccessFilter.filteredElement() + "]"
                );
            }
        };
        return (dependencyRequest, errors) -> {
            moduleClassVisitor.visit(dependencyRequest, errors);
            dependsOnVisitor.visit(dependencyRequest, errors);
            blackListVisitor.visit(dependencyRequest, errors);
            whiteListVisitor.visit(dependencyRequest, errors);
        };
    }

    private RequestVisitor blackListVisitor(final Class<?> module, Module moduleAnnotation) {

        Class<?>[] blackListConfigClasses = moduleAnnotation.notAccessibleFrom();

        if (blackListConfigClasses.length == 0) {
            return RequestVisitor.NONE;
        }
        
        RequestVisitor[] blackListVisitors = new RequestVisitor[blackListConfigClasses.length];
        
        for (int i = 0; i < blackListVisitors.length; i++) {
            
            Class<?> blackListConfigClass = blackListConfigClasses[i];
            
            final Class<?>[] blackListClasses;

            BlackList blackListModules =
                    blackListConfigClass.getAnnotation(BlackList.class);

            if (blackListModules != null) {
                blackListClasses = blackListModules.value();
            } else {
                blackListClasses = new Class<?>[]{};
            }

            final Qualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass);

            blackListVisitors[i] = (dependencyRequest, errors) -> {

                Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();
                for (Class<?> blackListClass : blackListClasses) {
                    if (blackListClass == requestingSourceModuleClass) {
                        errors.add("The module [" + requestingSourceModuleClass.getName()
                                + "] does not have access to the module [" + module.getName() + "].");
                    }
                }

                boolean qualifierMatch =
                        blackListQualifier.qualifiers().length > 0
                                && dependencyRequest.sourceQualifier().qualifiers().length > 0
                                && dependencyRequest.sourceQualifier().intersects(blackListQualifier);

                if (qualifierMatch) {
                    errors.add("The module [" + requestingSourceModuleClass.getName()
                            + "] does not have access to the module [" + module.getName() + "].");
                }
            };
            
        }

        return (dependencyRequest, errors) -> {
            for (RequestVisitor requestVisitor : blackListVisitors) {
                requestVisitor.visit(dependencyRequest, errors);
            }
        };
        
    }

    private RequestVisitor whiteListVisitor(final Class<?> module, Module moduleAnnotation) {

        Class<?>[] whiteListConfigClasses = moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClasses.length == 0) {
            return RequestVisitor.NONE;
        }

        RequestVisitor[] whiteListVisitors = new RequestVisitor[whiteListConfigClasses.length];

        for (int i = 0; i < whiteListVisitors.length; i++) {

            Class<?> whiteListConfigClass = whiteListConfigClasses[i];

            final Class<?>[] whiteListClasses;

            WhiteList whiteListModules =
                    whiteListConfigClass.getAnnotation(WhiteList.class);

            if (whiteListModules != null) {
                whiteListClasses = whiteListModules.value();
            } else {
                whiteListClasses = new Class<?>[]{};
            }

            final Qualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass);

            whiteListVisitors[i] = (dependencyRequest, errors) -> {

                Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();
                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == requestingSourceModuleClass || requestingSourceModuleClass == module) {
                        return;
                    }
                }

                boolean qualifierMatch = dependencyRequest.sourceQualifier().intersects(whiteListQualifier);
                if (!qualifierMatch || whiteListQualifier.qualifiers().length == 0) {
                    errors.add("The module [" + requestingSourceModuleClass.getName()
                            + "] does not have access to the module [" + module.getName() + "].");
                }

            };

        }

        return (dependencyRequest, errors) -> {
            for (RequestVisitor requestVisitor : whiteListVisitors) {
                requestVisitor.visit(dependencyRequest, errors);
            }
        };
    }

    private RequestVisitor dependsOnVisitor(final Class<?> module) {

        return (dependencyRequest, errors) -> {

            if (dependencyRequest.sourceProvision().overrides().allowImplicitModuleDependency()) {
                return;
            }

            ModuleMetadata requestSourceModule = dependencyRequest.sourceModule();

            if (module == requestSourceModule.moduleClass()) {
                return;
            }

            if (requestSourceModule.referencedModules().length == 0 && !requireExplicitModuleDependencies) {
                return;
            }

            for (Class<?> dependency : requestSourceModule.referencedModules()) {
                if (module == dependency) {
                    return;
                }
            }

            errors.add("The module [" + requestSourceModule.moduleClass().getName()
                    + "] does not have access to the module [" + module.getName() + "].");

        };

    }

    private RequestVisitor moduleResourceVisitor(ResourceMetadata<?> resourceMetadata,
                                                 ModuleMetadata moduleMetadata) {
        if (!resourceMetadata.isModule()) {
            return RequestVisitor.NONE;
        }
        return (dependencyRequest, dependencyResponse) -> {
            if (!dependencyRequest.sourceModule().equals(moduleMetadata)) {
                dependencyResponse.add("Module can only be requested by its providers"); // TODO
            }
        };
    }

    private AccessFilter<Class<?>> accessFilter(Resource resource) {

        ResourceMetadata<?> resourceMetadata = resource.metadata();
        ModuleMetadata moduleMetadata = resourceMetadata.moduleMetadata();

        // TODO this could be better
        AccessFilter<Class<?>> accessFilter;
        if (resourceMetadata.provider() instanceof Class) {
            accessFilter = AccessFilter.create(
                    (Class<?>) resourceMetadata.provider());
        } else if (resourceMetadata.provider() instanceof Member) {
            accessFilter = AccessFilter.create(
                    (AnnotatedElement & Member) resourceMetadata.provider());
        } else {
            accessFilter = AccessFilter.create(resourceMetadata.providerClass());
        }

        return new AccessFilter<Class<?>>() {
            @Override public AnnotatedElement filteredElement() {
                return accessFilter.filteredElement();
            }
            @Override public boolean isAccessibleTo(Class<?> target) {
                // supports library access
                return target == moduleMetadata.moduleClass() || accessFilter.isAccessibleTo(target);
            }
        };

    }

}

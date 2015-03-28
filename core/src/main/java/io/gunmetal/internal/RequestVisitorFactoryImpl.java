package io.gunmetal.internal;

import io.gunmetal.BlackList;
import io.gunmetal.Module;
import io.gunmetal.WhiteList;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.RequestVisitor;
import io.gunmetal.spi.ResourceMetadata;

/**
 * @author rees.byars
 */
class RequestVisitorFactoryImpl implements RequestVisitorFactory {

    private final QualifierResolver qualifierResolver;
    private final boolean requireExplicitModuleDependencies;

    RequestVisitorFactoryImpl(QualifierResolver qualifierResolver, boolean requireExplicitModuleDependencies) {
        this.qualifierResolver = qualifierResolver;
        this.requireExplicitModuleDependencies = requireExplicitModuleDependencies;
    }

    @Override public RequestVisitor resourceRequestVisitor(Resource resource,
                                                           GraphContext context) {
        ResourceMetadata<?> resourceMetadata = resource.metadata();
        ModuleMetadata moduleMetadata = resourceMetadata.moduleMetadata();
        Class<?> module = moduleMetadata.moduleClass();
        Module moduleAnnotation = moduleMetadata.moduleAnnotation();
        if (moduleAnnotation == Module.NONE) {
            return RequestVisitor.NONE;
        }
        final RequestVisitor blackListVisitor = blackListVisitor(module, moduleAnnotation, context);
        final RequestVisitor whiteListVisitor = whiteListVisitor(module, moduleAnnotation, context);
        final RequestVisitor dependsOnVisitor = dependsOnVisitor(module);
        final RequestVisitor moduleResourceVisitor =
                moduleResourceVisitor(resourceMetadata, moduleMetadata);
        final AccessFilter<Class<?>> classAccessFilter = AccessFilter.create(moduleAnnotation.access(), module);
        final RequestVisitor moduleClassVisitor = (dependencyRequest, errors) -> {
            if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
                errors.add(
                        "The module [" + dependencyRequest.sourceModule().moduleClass().getName()
                                + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
                );
            }
        };
        return (dependencyRequest, errors) -> {
            moduleClassVisitor.visit(dependencyRequest, errors);
            dependsOnVisitor.visit(dependencyRequest, errors);
            blackListVisitor.visit(dependencyRequest, errors);
            whiteListVisitor.visit(dependencyRequest, errors);
            moduleResourceVisitor.visit(dependencyRequest, errors);
        };
    }

    private RequestVisitor blackListVisitor(final Class<?> module, Module moduleAnnotation, GraphContext context) {

        Class<? extends BlackList> blackListConfigClass =
                moduleAnnotation.notAccessibleFrom();

        if (blackListConfigClass == BlackList.class) {
            return RequestVisitor.NONE;
        }

        final Class<?>[] blackListClasses;

        BlackList.Modules blackListModules =
                blackListConfigClass.getAnnotation(BlackList.Modules.class);

        if (blackListModules != null) {
            blackListClasses = blackListModules.value();
        } else {
            blackListClasses = new Class<?>[]{};
        }

        final Qualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass, context.errors());

        return (dependencyRequest, errors) -> {

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

    private RequestVisitor whiteListVisitor(final Class<?> module, Module moduleAnnotation, GraphContext context) {

        Class<? extends WhiteList> whiteListConfigClass =
                moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClass == WhiteList.class) {
            return RequestVisitor.NONE;
        }

        final Class<?>[] whiteListClasses;

        WhiteList.Modules whiteListModules =
                whiteListConfigClass.getAnnotation(WhiteList.Modules.class);

        if (whiteListModules != null) {
            whiteListClasses = whiteListModules.value();
        } else {
            whiteListClasses = new Class<?>[]{};
        }

        final Qualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass, context.errors());

        return (dependencyRequest, errors) -> {

            Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();
            for (Class<?> whiteListClass : whiteListClasses) {
                if (whiteListClass == requestingSourceModuleClass) {
                    return;
                }
            }

            boolean qualifierMatch = dependencyRequest.sourceQualifier().intersects(whiteListQualifier);
            if (!qualifierMatch) {
                errors.add("The module [" + requestingSourceModuleClass.getName()
                        + "] does not have access to the module [" + module.getName() + "].");
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

}

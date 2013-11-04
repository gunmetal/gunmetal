package io.gunmetal.builder;

import java.util.List;

/**
 * @author rees.byars
 */
interface ModuleBuilder {
    List<AccessRestrictedComponentAdapter<?>> build(Class<?> module, InternalProvider internalProvider);
}

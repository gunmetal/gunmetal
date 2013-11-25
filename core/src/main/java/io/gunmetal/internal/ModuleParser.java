package io.gunmetal.internal;

import java.util.List;

/**
 * @author rees.byars
 */
interface ModuleParser {
    List<ComponentAdapter<?>> parse(Class<?> module, InternalProvider provider);
}

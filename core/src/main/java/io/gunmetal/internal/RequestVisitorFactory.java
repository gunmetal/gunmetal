package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.spi.RequestVisitor;

/**
 * @author rees.byars
 */
public interface RequestVisitorFactory {
    RequestVisitor moduleRequestVisitor(Class<?> module,
                                        Module moduleAnnotation,
                                        GraphContext context);
}

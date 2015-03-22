package io.gunmetal.internal;

import io.gunmetal.spi.RequestVisitor;

/**
 * @author rees.byars
 */
interface RequestVisitorFactory {

    RequestVisitor resourceRequestVisitor(Resource resource, GraphContext context);

}

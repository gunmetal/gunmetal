package io.gunmetal.compiler;

/**
 * @author rees.byars
 */
interface Builder<R> extends AnnotationVisitor {

    R build();

}

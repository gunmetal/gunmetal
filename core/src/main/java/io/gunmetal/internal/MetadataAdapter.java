package io.gunmetal.internal;

import java.lang.annotation.Annotation;

/**
 * @author rees.byars
 */
interface MetadataAdapter {

    Class<? extends Annotation> qualifierAnnotation();

    Class<? extends Annotation> scopeAnnotation();

}

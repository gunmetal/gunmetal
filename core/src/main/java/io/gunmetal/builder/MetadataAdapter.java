package io.gunmetal.builder;

import java.lang.annotation.Annotation;

/**
 * @author rees.byars
 */
interface MetadataAdapter {
    Class<? extends Annotation> getQualifierAnnotation();
    Class<? extends Annotation> getScopeAnnotation();
}

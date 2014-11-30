package io.gunmetal.compiler;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;

/**
 * @author rees.byars
 */
class Utils {

    static boolean isAnnotationPresent(Element element, Class<? extends Annotation> annotationType) {
        return element.getAnnotation(annotationType) != null;
    }


}

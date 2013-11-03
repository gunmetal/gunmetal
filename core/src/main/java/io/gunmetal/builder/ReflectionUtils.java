package io.gunmetal.builder;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public class ReflectionUtils {

    static Object[] getQualifiers(Class<?> cls, Class<? extends Annotation> qualifierAnnotation) {
        List<Object> qualifiers = new LinkedList<Object>();
        for (Annotation annotation : cls.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(qualifierAnnotation)) {
                qualifiers.add(annotation);
            }
        }
        return qualifiers.toArray();
    }

}

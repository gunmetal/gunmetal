package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class DependencyUtils {

    static Dependency[] forParams(
            Method method,
            QualifierResolver qualifierResolver,
            Qualifier parentQualifier) {
        Type[] paramTypes = method.getGenericParameterTypes();
        final Annotation[][] methodParameterAnnotations
                = method.getParameterAnnotations();
        Dependency[] dependencies = new Dependency[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Type paramType = paramTypes[i];
            Annotation[] annotations = methodParameterAnnotations[i];
            AnnotatedElement annotatedElement =
                    new AnnotatedElement() {
                        @Override public <TT extends Annotation> TT getAnnotation(Class<TT> annotationClass) {
                            for (Annotation annotation : annotations) {
                                if (annotationClass.isInstance(annotation)) {
                                    return annotationClass.cast(annotation);
                                }
                            }
                            return null;
                        }
                        @Override public Annotation[] getAnnotations() {
                            return annotations;
                        }

                        @Override public Annotation[] getDeclaredAnnotations() {
                            return annotations;
                        }
                    };
            Qualifier paramQualifier = qualifierResolver
                    .resolveDependencyQualifier(
                            annotatedElement,
                            parentQualifier);
            Dependency paramDependency =
                    Dependency.from(paramQualifier, paramType);
            dependencies[i] = paramDependency;
        }
        return dependencies;
    }

    static Dependency[] forParamTypes(
            Method method,
            QualifierResolver qualifierResolver,
            Qualifier parentQualifier) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Dependency[] dependencies = new Dependency[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            Qualifier paramQualifier = qualifierResolver
                    .resolveDependencyQualifier(
                            paramType,
                            parentQualifier);
            Dependency paramDependency =
                    Dependency.from(paramQualifier, paramType);
            dependencies[i] = paramDependency;
        }
        return dependencies;
    }

}

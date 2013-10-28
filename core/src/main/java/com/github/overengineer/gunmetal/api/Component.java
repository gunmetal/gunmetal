package com.github.overengineer.gunmetal.api;

import com.github.overengineer.gunmetal.metadata.Scope;
import com.github.overengineer.gunmetal.util.VisibilityAdapter;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rees.byars
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {
    Class<?> type();
    Class<? extends Annotation> scope() default Scope.class; //undefined scope
    VisibilityAdapter.AccessLevel access() default VisibilityAdapter.AccessLevel.UNDEFINED;
    Class<?>[] targets() default {};
}

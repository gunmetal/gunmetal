package com.github.overengineer.gunmetal.api;

import com.github.overengineer.gunmetal.ComponentPostProcessor;
import com.github.overengineer.gunmetal.proxy.aop.Aspect;

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
public @interface ConfigurationModule {
    Option[] requiresOptions() default {};
    Class<? extends Aspect>[] aspects() default {};
    Class<? extends ComponentPostProcessor>[] postProcessors() default {};
    ScopeHandler[] scopes() default {};
    Class<?>[] modules() default {};
}

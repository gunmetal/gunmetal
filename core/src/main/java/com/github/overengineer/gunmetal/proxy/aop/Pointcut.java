package com.github.overengineer.gunmetal.proxy.aop;


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
public @interface Pointcut {

    Class[] classes() default { };

    Class<?> returnType() default PlaceHolder.class;

    Class[] paramterTypes() default PlaceHolder.class;

    Class<? extends Annotation>[] annotations() default { };

    String methodNameExpression() default "";

    String classNameExpression() default "";

    public @interface PlaceHolder { }
}

package io.gunmetal;

import com.github.overengineer.gunmetal.scope.ScopedComponentStrategyProvider;

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
public @interface ScopeHandler {
    Class<? extends ScopedComponentStrategyProvider> type();
    Class<? extends Annotation> annotation();
}

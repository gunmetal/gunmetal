package io.gunmetal.testmocks.dongle.scope;

import io.gunmetal.Scope;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author rees.byars
 */
@Retention(RetentionPolicy.RUNTIME)
@Scope(scopeEnum = Scopes.class, name = "THREAD")
public @interface Thread {
}

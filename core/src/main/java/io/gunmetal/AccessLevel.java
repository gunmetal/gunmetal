package io.gunmetal;

import java.lang.reflect.Modifier;

/**
 * @author rees.byars
 */
public enum  AccessLevel {

    PRIVATE,
    PACKAGE_PRIVATE,
    PROTECTED,
    PUBLIC,
    UNDEFINED;

    public static AccessLevel get(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return PUBLIC;
        } else if (Modifier.isPrivate(modifiers)) {
            return PRIVATE;
        } else if (Modifier.isProtected(modifiers)) {
            return PROTECTED;
        } else {
            return PACKAGE_PRIVATE;
        }
    }
}

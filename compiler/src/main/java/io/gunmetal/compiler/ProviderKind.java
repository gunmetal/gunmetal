package io.gunmetal.compiler;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * @author rees.byars
 */
enum ProviderKind {

    INSTANCE_CONSTRUCTOR,

    STATIC_CONSTRUCTOR,

    INSTANCE_METHOD,

    STATIC_METHOD;

    static ProviderKind fromElement(Element providerElement) {
        switch (providerElement.getKind()) {
            case CONSTRUCTOR: return ProviderKind.STATIC_CONSTRUCTOR; // TODO instance??
            case METHOD: {
                if (providerElement.getModifiers().contains(Modifier.STATIC)) {
                    return ProviderKind.STATIC_METHOD;
                }
                return ProviderKind.INSTANCE_METHOD;
            }
            default: throw new IllegalArgumentException("Invalid provider element kind: "
                    + providerElement.getKind());
        }
    }

}

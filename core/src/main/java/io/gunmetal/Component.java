package io.gunmetal;

import io.gunmetal.internal.ComponentTemplate;
import io.gunmetal.spi.GunmetalComponent;

/**
 * @author rees.byars
 */
public interface Component {

    public static <T> T buildTemplate(Class<T> componentFactoryInterface) {
        return buildTemplate(new GunmetalComponent.Default(), componentFactoryInterface);
    }

    public static <T> T buildTemplate(GunmetalComponent gunmetalComponent, Class<T> componentFactoryInterface) {
        return ComponentTemplate.build(gunmetalComponent, componentFactoryInterface);
    }

    public static <T> T build(Class<T> componentClass) {
        return build(new GunmetalComponent.Default(), componentClass);
    }

    public static <T> T build(GunmetalComponent gunmetalComponent, Class<T> componentClass) {
        return ComponentTemplate.buildComponent(gunmetalComponent, componentClass);
    }

}

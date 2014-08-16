package io.gunmetal.spi;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public interface ProvisionMetadataResolver {

    ProvisionMetadata<Method> resolveMetadata(Method method, ModuleMetadata moduleMetadata, Errors errors);

    ProvisionMetadata<Class<?>> resolveMetadata(Class<?> cls, ModuleMetadata moduleMetadata, Errors errors);

}

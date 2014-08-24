package io.gunmetal.spi;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public interface ResourceMetadataResolver {

    ResourceMetadata<Method> resolveMetadata(Method method, ModuleMetadata moduleMetadata, Errors errors);

    ResourceMetadata<Class<?>> resolveMetadata(Class<?> cls, ModuleMetadata moduleMetadata, Errors errors);

}

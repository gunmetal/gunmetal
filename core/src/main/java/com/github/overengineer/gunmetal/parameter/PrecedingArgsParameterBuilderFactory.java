package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.util.ConstructorRefImpl;
import com.github.overengineer.gunmetal.util.MethodRefImpl;
import com.github.overengineer.gunmetal.util.ParameterRef;
import com.github.overengineer.gunmetal.util.ParameterizedFunction;
import com.github.overengineer.gunmetal.util.ParameterRefImpl;
import com.github.overengineer.gunmetal.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class PrecedingArgsParameterBuilderFactory implements ParameterBuilderFactory {

    private final MetadataAdapter metadataAdapter;

    public PrecedingArgsParameterBuilderFactory(MetadataAdapter metadataAdapter) {
        this.metadataAdapter = metadataAdapter;
    }

    @Override
    public <T> ParameterBuilder<T> create(Class<T> injectionTarget, Constructor<T> constructor, Class[] providedArgs) {
        return create(injectionTarget, new ConstructorRefImpl<T>(constructor), providedArgs);
    }

    @Override
    public <T> ParameterBuilder<T> create(Class<T> injectionTarget, Method method, Class[] providedArgs) {
        return create(injectionTarget, new MethodRefImpl(method), providedArgs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParameterBuilder<T> create(Class<?> injectionTarget, ParameterizedFunction parameterizedFunction, Class[] providedArgs) {

        Type[] genericParameterTypes = parameterizedFunction.getParameterTypes();
        Annotation[][] annotations = parameterizedFunction.getParameterAnnotations();

        ParameterProxy[] parameterProxies = new ParameterProxy[genericParameterTypes.length - providedArgs.length];
        for (int i = 0; i < parameterProxies.length; i++) {
            int parameterIndex = i + providedArgs.length;
            ParameterRef parameterRef = new ParameterRefImpl(parameterizedFunction, parameterIndex);
            parameterProxies[i] = createProxy(injectionTarget, parameterRef, annotations[i + providedArgs.length]);
        }

        return createBuilder(parameterProxies);
    }

    @SuppressWarnings("unchecked")
    protected  <T> ParameterProxy<T> createProxy(Class<?> injectionTarget, ParameterRef parameterRef, Annotation[] annotations) {

        Type parameterType = parameterRef.getType();

        Dependency dependency = Smithy.forge(parameterRef, metadataAdapter.getQualifier(parameterType, annotations));

        if (ReflectionUtil.getRawClass(parameterType).isAssignableFrom(injectionTarget)) {
            return new DecoratorParameterProxy<T>(dependency, injectionTarget);
        }

        return new ComponentParameterProxy<T>(dependency);

    }

    protected <T> ParameterBuilder<T> createBuilder(ParameterProxy[] proxies) {

        boolean decorator = false;

        for (ParameterProxy proxy : proxies) {
            if (proxy instanceof DecoratorParameterProxy) {
                decorator = true;
                break;
            }
        }

        return new PrecedingArgsParameterBuilder<T>(proxies, decorator);

    }

}

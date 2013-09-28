package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.util.ParameterizedFunction;
import com.github.overengineer.gunmetal.util.ParameterTypeRef;
import com.github.overengineer.gunmetal.util.ReflectionUtil;
import com.github.overengineer.gunmetal.util.TypeRef;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class PrecedingArgsParameterBuilderFactory implements ParameterBuilderFactory {

    private static final long serialVersionUID = -882200538480248040L;
    private final MetadataAdapter metadataAdapter;

    public PrecedingArgsParameterBuilderFactory(MetadataAdapter metadataAdapter) {
        this.metadataAdapter = metadataAdapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParameterBuilder<T> create(Class<T> injectionTarget, ParameterizedFunction parameterizedFunction, Class[] providedArgs) {

        Type[] genericParameterTypes = parameterizedFunction.getParameterTypes();
        Annotation[][] annotations = parameterizedFunction.getParameterAnnotations();

        ParameterProxy[] parameterProxies = new ParameterProxy[genericParameterTypes.length - providedArgs.length];
        for (int i = 0; i < parameterProxies.length; i++) {
            int parameterIndex = i + providedArgs.length;
            TypeRef parameterRef = new ParameterTypeRef(parameterizedFunction, parameterIndex);
            parameterProxies[i] = createProxy(injectionTarget, parameterRef, annotations[i + providedArgs.length]);
        }

        return createBuilder(parameterProxies);
    }

    @SuppressWarnings("unchecked")
    protected  <T> ParameterProxy<T> createProxy(Class<?> injectionTarget, TypeRef parameterRef, Annotation[] annotations) {

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

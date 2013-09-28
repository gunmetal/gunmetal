package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.util.ConstructorProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class DefaultConstructorResolver implements ConstructorResolver {

    private static final long serialVersionUID = 8924169648536416290L;
    private final MetadataAdapter metadataAdapter;

    public DefaultConstructorResolver(MetadataAdapter metadataAdapter) {
        this.metadataAdapter = metadataAdapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ConstructorProxy<T> resolveConstructor(Class<T> type, Class ... providedArgs) {
        Type[] genericParameterTypes = {};
        Constructor<T> constructor = null;
        for (Constructor candidateConstructor : type.getDeclaredConstructors()) {
            if (metadataAdapter.isValidConstructor(candidateConstructor)) {
                Type[] candidateTypes = candidateConstructor.getGenericParameterTypes();
                if (candidateTypes.length >= genericParameterTypes.length && candidateTypes.length >= providedArgs.length) { //TODO use param matching util
                    constructor = candidateConstructor;
                    genericParameterTypes = candidateTypes;
                }
            }
        }
        assert constructor != null : "Could not find a suitable constructor in [" + type + "]";
        return ConstructorProxy.Factory.create(constructor);
    }

}

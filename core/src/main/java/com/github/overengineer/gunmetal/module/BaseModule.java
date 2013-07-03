package com.github.overengineer.gunmetal.module;

import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.key.Qualifier;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.scope.Scope;
import com.github.overengineer.gunmetal.scope.Scopes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
public abstract class BaseModule implements Module {

    private final List<Mapping<?>> mappings = new LinkedList<Mapping<?>>();
    private final Map<Dependency, Class> nonManagedComponentFactories = new HashMap<Dependency, Class>();
    private Scope defaultScope = Scopes.UNDEFINED;
    private Object defaultQualifier = Qualifier.NONE;
    private boolean useAutoMapping = false;

    @Override
    public final List<Mapping<?>> getMappings() {
        return mappings;
    }

    @Override
    public Map<Dependency, Class> getNonManagedComponentFactories() {
        return nonManagedComponentFactories;
    }

    protected  <V> MutableMapping<V> use(Class<V> implementationClass) {
        return prepare(new TypeMapping<V>(implementationClass));
    }

    protected  <V> MutableMapping<V> use(Generic<V> implementationGeneric) {
        return prepare(new GenericMapping<V>(implementationGeneric));
    }

    protected  <V> MutableMapping<V> use(V implementation) {
        return prepare(new InstanceMappingImpl<V>(implementation));
    }

    protected NonManagedComponentFactoryMapper registerNonManagedComponentFactory(Class<?> factoryType) {
        return new NonManagedComponentFactoryMapper(factoryType);
    }

    private <V> MutableMapping<V> prepare(MutableMapping<V> mapping) {
        mappings.add((Mapping) mapping);
        mapping.withScope(defaultScope);
        mapping.withQualifier(defaultQualifier);
        if (useAutoMapping) {
            mapping.forAllTypes();
        }
        return mapping;
    }

    public class NonManagedComponentFactoryMapper {
        private final Dependency<?> dependency;
        public <T> NonManagedComponentFactoryMapper(Class<T> key) {
            this.dependency = Smithy.forge(key);
        }
        public NonManagedComponentFactoryMapper(Dependency dependency) {
            this.dependency = dependency;
        }
        public void toProduce(Class value) {
            nonManagedComponentFactories.put(dependency, value);
        }
    }

    protected BaseModule defaultScope(Scope scope) {
        defaultScope = scope;
        return this;
    }

    protected BaseModule defaultQualifier(Object qualifier) {
        defaultQualifier = qualifier;
        return this;
    }

    protected BaseModule useAutoMapping() {
        useAutoMapping = true;
        return this;
    }

}

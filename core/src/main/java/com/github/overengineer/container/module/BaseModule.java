package com.github.overengineer.container.module;

import com.github.overengineer.container.key.ClassKey;
import com.github.overengineer.container.key.Generic;
import com.github.overengineer.container.key.Key;
import com.github.overengineer.container.key.Qualifier;
import com.github.overengineer.container.scope.Scope;
import com.github.overengineer.container.scope.Scopes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
public abstract class BaseModule implements Module {

    private final List<Mapping<?>> mappings = new LinkedList<Mapping<?>>();
    private final Map<Key, Class> nonManagedComponentFactories = new HashMap<Key, Class>();
    private Scope defaultScope = Scopes.SINGLETON;
    private Object defaultQualifier = Qualifier.NONE;
    private boolean useAutoMapping = false;

    public BaseModule() {
        configure();
    }

    @Override
    public final List<Mapping<?>> getMappings() {
        return mappings;
    }

    @Override
    public Map<Key, Class> getNonManagedComponentFactories() {
        return nonManagedComponentFactories;
    }

    protected abstract void configure();

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
        private final Key<?> key;
        public <T> NonManagedComponentFactoryMapper(Class<T> key) {
            this.key = new ClassKey<T>(key);
        }
        public NonManagedComponentFactoryMapper(Key key) {
            this.key = key;
        }
        public void toProduce(Class value) {
            nonManagedComponentFactories.put(key, value);
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

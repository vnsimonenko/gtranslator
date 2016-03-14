package com.gtranslator.storage.batch;

import com.gtranslator.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class BatchEntityContext {
    private Map<Class<?>, Collection<Object>> context = new HashMap<>();

    private BatchEntityContext() {
    }

    static BatchEntityContext create() {
        return new BatchEntityContext();
    }

    void put(Class<?> clazz, Object value) throws InstantiationException, IllegalAccessException {
        Utils.getMultiplicityValueFromMap(context, clazz, HashSet::new).add(value);
    }

    <T> Collection<T> get(Class<T> clazz) {
        return (Collection<T>) context.get(clazz);
    }
}

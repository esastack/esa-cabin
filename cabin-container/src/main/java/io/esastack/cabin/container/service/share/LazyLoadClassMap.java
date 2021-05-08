/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.esastack.cabin.container.service.share;

import io.esastack.cabin.api.domain.Module;
import io.esastack.cabin.container.service.loader.LibModuleClassLoader;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LazyLoadClassMap implements Map<String, Class<?>> {

    private final ConcurrentMap<String, Class<?>> cachedClasses;

    private final ConcurrentMap<String, Module> classToModuleMap;

    public LazyLoadClassMap(final int initCapacity) {
        this.cachedClasses = new ConcurrentHashMap<>(initCapacity);
        this.classToModuleMap = new ConcurrentHashMap<>(initCapacity);
    }

    @Override
    public int size() {
        return classToModuleMap.size();
    }

    @Override
    public boolean isEmpty() {
        return classToModuleMap.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return classToModuleMap.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> get(final Object key) {
        final Class<?> cachedClass = cachedClasses.get(key);
        if (cachedClass != null) {
            return cachedClass;
        }

        final Module module = classToModuleMap.get(key);
        if (module == null) {
            return null;
        }

        try {
            Class<?> result = null;
            final LibModuleClassLoader libModuleClassLoader = (LibModuleClassLoader) module.getClassLoader();
            if (libModuleClassLoader != null) {
                result = libModuleClassLoader.loadClassFromClasspath((String) key);
                if (result != null) {
                    cachedClasses.put((String) key, result);
                }
            }
            return result;
        } catch (Throwable e) {
            //NOP
        }
        return null;
    }

    @Override
    public Class<?> put(final String key, final Class<?> value) {
        return cachedClasses.put(key, value);
    }

    @Override
    public Class<?> putIfAbsent(final String key, final Class<?> value) {
        return cachedClasses.putIfAbsent(key, value);
    }

    @Override
    public Class<?> remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Class<?>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Class<?>> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, Class<?>>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public Module putIfAbsent(final String className, final Module module) {
        return classToModuleMap.putIfAbsent(className, module);
    }
}

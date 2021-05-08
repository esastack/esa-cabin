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
package io.esastack.cabin.common.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Used by the biz code, for Cabin state detecting.
 * Must be loaded and initialized by the BizModuleClassLoader.
 */
public class CabinContainerUtil {

    private static volatile Object cabinContainer;

    private static volatile ClassLoader bizClassLoader;

    private static synchronized void init(final Object container, final ClassLoader classLoader) {
        if (isStarted()) {
            throw new RuntimeException("Could not init CabinContainer more than once");
        }
        cabinContainer = container;
        bizClassLoader = classLoader;
    }

    public static boolean isStarted() {
        return cabinContainer != null && bizClassLoader != null;
    }

    public static boolean moduleLoaded(final String moduleName) {
        return (boolean) delegate2CabinContainer("moduleLoaded", new Object[]{moduleName});
    }

    public static ClassLoader getBizClassLoader() {
        if (bizClassLoader == null) {
            bizClassLoader = (ClassLoader) delegate2CabinContainer(
                    "getBizModuleClassLoader", new Object[0]);
        }
        return bizClassLoader;
    }

    /**
     * CabinContainer would have not overloaded methods
     */
    private static Object delegate2CabinContainer(final String methodName, final Object[] arguments) {
        if (cabinContainer == null) {
            throw new IllegalStateException("Cabin Container has not been setup!");
        }
        try {
            final Method[] methods = cabinContainer.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return method.invoke(cabinContainer, arguments);
                }
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    String.format("Failed to invoke method %s of CabinContainer!", methodName), e.getCause());
        } catch (Throwable ex) {
            throw new RuntimeException(
                    String.format("Failed to invoke method %s of CabinContainer!", methodName), ex);
        }
        throw new RuntimeException(
                String.format("There is no method named %s of CabinContainer!", methodName));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Class<?>> getExportedClasses() {
        return (Map<String, Class<?>>) delegate2CabinContainer("getExportedClasses", new Object[0]);
    }
}

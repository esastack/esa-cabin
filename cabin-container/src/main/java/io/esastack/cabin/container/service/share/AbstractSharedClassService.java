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
import io.esastack.cabin.api.service.share.SharedClassService;
import io.esastack.cabin.common.exception.CabinLoaderException;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.domain.LibModule;
import io.esastack.cabin.container.service.loader.LibModuleClassLoader;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Different modules could export same packages, but should not export same classes!
 */
public abstract class AbstractSharedClassService implements SharedClassService {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(AbstractSharedClassService.class);

    private static final Map<String, Map<LibModule, Object>> packageToModuleMap = new ConcurrentHashMap<>();

    private static final Object sentinel = new Object();

    @Override
    public void addSharedPackage(final String packageName, final Module module) {
        if (CabinStringUtil.isBlank(packageName)) {
            return;
        }

        final Map<LibModule, Object> modules =
                packageToModuleMap.computeIfAbsent(packageName, name -> new ConcurrentHashMap<>());
        modules.put((LibModule) module, sentinel);
    }

    @Override
    public Class<?> getSharedClass(final String className) {
        Class<?> clazz = getSharedClass0(className);
        if (clazz != null) {
            return clazz;
        }

        int index = className.lastIndexOf(".");
        while (index > 0) {
            final String packageName = className.substring(0, index);
            final Map<LibModule, Object> modules = packageToModuleMap.get(packageName);
            if (modules != null && !modules.isEmpty()) {
                Class<?> prevLoadedClass = null;
                LibModule prevLoadedModule = null;
                for (LibModule libModule : modules.keySet()) {
                    final LibModuleClassLoader classLoader = (LibModuleClassLoader) libModule.getClassLoader();
                    try {
                        clazz = classLoader.loadClassFromClasspath(className);
                    } catch (CabinLoaderException e) {
                        throw new CabinRuntimeException(String.format("Failed to export class %s from module %s",
                                className, libModule.getName()));
                    }
                    if (clazz != null) {
                        if (prevLoadedClass != null) {
                            throw new CabinRuntimeException(
                                    String.format("Class export conflicted, %s is exported by module %s and %s",
                                            className, prevLoadedModule.getName(), libModule.getName()));
                        } else {
                            prevLoadedClass = clazz;
                            prevLoadedModule = libModule;
                        }
                    }
                }
                if (prevLoadedClass != null) {
                    LOGGER.info("Trying to add class {} exported by Module {} to sharedClassService!",
                            className, prevLoadedModule.getName());
                    addSharedClass(className, prevLoadedClass);
                    return prevLoadedClass;
                }
            }
            index = packageName.lastIndexOf(".");
        }
        return null;
    }

    protected abstract Class<?> getSharedClass0(final String className);
}

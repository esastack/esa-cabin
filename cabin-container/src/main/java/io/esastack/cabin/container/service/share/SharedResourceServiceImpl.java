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

import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.api.service.share.SharedResourceService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SharedResourceServiceImpl implements SharedResourceService {

    private final ConcurrentHashMap<String, List<ClassLoader>> resourceClassLoaderMap = new ConcurrentHashMap<>();
    private volatile LibModuleLoadService libModuleLoadService;

    public void setLibModuleLoadService(final LibModuleLoadService service) {
        libModuleLoadService = service;
    }

    @Override
    public List<ClassLoader> getResourceClassLoaders(final String name) {
        return resourceClassLoaderMap.get(name);
    }

    @Override
    public void addExportClassLoader(final String name, final ClassLoader classLoader) {
        final List<ClassLoader> classLoaders = resourceClassLoaderMap.computeIfAbsent(name, s -> new ArrayList<>());
        classLoaders.add(classLoader);
    }
}

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
package io.esastack.cabin.api.service.share;

import io.esastack.cabin.api.domain.Module;

import java.util.Map;

/**
 * The implementation should save the mapping of classname to libModule, classname to class;
 */
public interface SharedClassService {

    int DEFAULT_CACHE_MAP_CAPACITY = 8192;

    void addSharedClass(final String className, final Class<?> clazz);

    void addSharedClass(final String className, final Module module);

    void addSharedPackage(final String packageName, final Module module);

    void preLoadAllSharedClasses();

    Class<?> getSharedClass(final String className);

    Map<String, Class<?>> getSharedClassMap();

    int getSharedClassCount();

    boolean containsClass(final String className);
}

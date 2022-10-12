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
package io.esastack.cabin.container.service.loader;

import io.esastack.cabin.api.service.loader.BizModuleClassLoaderParam;
import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.api.service.loader.LibModuleClassLoaderParam;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.util.CabinStringUtil;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClassLoaderServiceImpl implements ClassLoaderService {

    private final ClassLoader extClassLoader;

    private final ClassLoader cabinClassLoader;

    private final ConcurrentMap<String, ClassLoader> javaAgentClassLoaders = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, ClassLoader> libModuleClassLoaders = new ConcurrentHashMap<>();

    private volatile ClassLoader bizModuleClassLoader;

    public ClassLoaderServiceImpl() {
        ClassLoader ext = ClassLoader.getSystemClassLoader().getParent();
        while (ext.getParent() != null) {
            ext = ext.getParent();
        }
        this.extClassLoader = ext;
        this.cabinClassLoader = this.getClass().getClassLoader();
    }

    @Override
    public ClassLoader getExtClassLoader() {
        return extClassLoader;
    }

    @Override
    public ClassLoader getCabinClassLoader() {
        return cabinClassLoader;
    }

    @Override
    public Map<String, ClassLoader> getJavaAgentModuleClassLoaders() {
        return javaAgentClassLoaders;
    }

    @Override
    public ClassLoader createJavaAgentModuleClassLoader(final URL agentUrl) {
        final ClassLoader classLoader = new JavaAgentClassLoader(agentUrl);
        javaAgentClassLoaders.put(agentUrl.toExternalForm(), classLoader);
        return classLoader;
    }

    @Override
    public ClassLoader getBizModuleClassLoader() {
        return bizModuleClassLoader;
    }

    @Override
    public ClassLoader createBizModuleClassLoader(final BizModuleClassLoaderParam param) {
        final BizModuleClassLoader bizModuleClassLoader;
        if (param.isUnitTest()) {
            bizModuleClassLoader = new UnitTestModuleClassLoader(param.getUrls());
        } else {
            bizModuleClassLoader = new BizModuleClassLoader(param.getUrls());
        }
        this.bizModuleClassLoader = bizModuleClassLoader;
        return bizModuleClassLoader;
    }

    @Override
    public ClassLoader getLibModuleClassLoader(final String moduleName) {
        if (CabinStringUtil.isBlank(moduleName)) {
            return null;
        }
        return this.libModuleClassLoaders.get(moduleName);
    }

    @Override
    public ClassLoader createLibModuleClassLoader(final LibModuleClassLoaderParam param) {
        if (param == null) {
            throw new CabinRuntimeException("Param is null");
        }

        if (CabinStringUtil.isBlank(param.getModuleName())) {
            throw new CabinRuntimeException("Module name is blank");
        }

        if (param.getUrls() == null || param.getUrls().length == 0) {
            throw new CabinRuntimeException("Module url is empty");
        }

        final URL[] urls = param.getUrls();
        LibModuleClassLoader libModuleClassLoader = new LibModuleClassLoader(param.getModuleName(), urls);
        libModuleClassLoader.setLoadFromBizClassLoader(param.isLoadFromBizClassLoader());
        libModuleClassLoader.setProvidedClasses(param.getProvidedClassList());
        libModuleClassLoader.setImportClasses(param.getImportClassList());
        libModuleClassLoader.setImportPackages(param.getImportPackageList());
        libModuleClassLoader.setImportResources(param.getImportResources());
        this.libModuleClassLoaders.put(param.getModuleName(), libModuleClassLoader);
        return libModuleClassLoader;
    }

    @Override
    public ClassLoader destroyLibModuleClassLoader(String moduleName) {
        return this.libModuleClassLoaders.remove(moduleName);
    }
}

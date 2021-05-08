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
package io.esastack.cabin.api.service.loader;

import java.net.URL;
import java.util.List;

public class LibModuleClassLoaderParam {

    private final URL[] urls;

    private final String moduleName;

    private final List<String> providedClassList;

    private final List<String> importClassList;

    private final List<String> importPackageList;

    private final List<String> importResources;

    private final boolean loadFromBizClassLoader;

    private final boolean loadFromSystemClassLoader;

    private LibModuleClassLoaderParam(Builder builder) {
        this.urls = builder.urls;
        this.moduleName = builder.moduleName;
        this.providedClassList = builder.providedClassList;
        this.importClassList = builder.importClassList;
        this.importPackageList = builder.importPackageList;
        this.importResources = builder.importResources;
        this.loadFromBizClassLoader = builder.loadFromBizClassLoader;
        this.loadFromSystemClassLoader = builder.loadFromSystemClassLoader;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public boolean isLoadFromBizClassLoader() {
        return loadFromBizClassLoader;
    }

    public boolean isLoadFromSystemClassLoader() {
        return loadFromSystemClassLoader;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<String> getProvidedClassList() {
        return providedClassList;
    }

    public List<String> getImportClassList() {
        return importClassList;
    }

    public List<String> getImportPackageList() {
        return importPackageList;
    }

    public List<String> getImportResources() {
        return importResources;
    }

    public URL[] getUrls() {
        return urls;
    }

    public static class Builder {

        private URL[] urls;

        private String moduleName;

        private List<String> providedClassList;

        private List<String> importClassList;

        private List<String> importPackageList;

        private List<String> importResources;

        private boolean loadFromBizClassLoader;

        private boolean loadFromSystemClassLoader;

        public Builder urls(URL[] urls) {
            this.urls = urls;
            return this;
        }

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder providedClassList(List<String> providedClassList) {
            this.providedClassList = providedClassList;
            return this;
        }

        public Builder importClassList(List<String> importClassList) {
            this.importClassList = importClassList;
            return this;
        }

        public Builder importPackageList(List<String> importPackageList) {
            this.importPackageList = importPackageList;
            return this;
        }

        public Builder importResources(List<String> importResources) {
            this.importResources = importResources;
            return this;
        }

        public Builder loadFromBizClassLoader(boolean loadFromBizClassLoader) {
            this.loadFromBizClassLoader = loadFromBizClassLoader;
            return this;
        }

        public Builder loadFromSystemClassLoader(boolean loadFromSystemClassLoader) {
            this.loadFromSystemClassLoader = loadFromSystemClassLoader;
            return this;
        }

        public LibModuleClassLoaderParam build() {
            return new LibModuleClassLoaderParam(this);
        }
    }
}


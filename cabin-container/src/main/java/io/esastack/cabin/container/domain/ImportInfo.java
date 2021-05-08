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
package io.esastack.cabin.container.domain;

import io.esastack.cabin.common.util.CabinStringUtil;

import java.util.ArrayList;
import java.util.List;

public class ImportInfo {

    private final List<String> importClassList = new ArrayList<>();
    private final List<String> importPackageList = new ArrayList<>();
    private final List<String> importResources = new ArrayList<>();
    private volatile boolean loadFromBizClassLoader;
    private volatile boolean loadFromSystemClassLoader;

    public void addClasses(final List<String> classList) {
        if (classList != null) {
            for (String clazz : classList) {
                addClass(clazz);
            }
        }
    }

    public void addClass(final String clazz) {
        if (!CabinStringUtil.isBlank(clazz)) {
            importClassList.add(clazz.trim());
        }
    }

    public void addPackages(final List<String> packageList) {
        if (packageList != null) {
            for (String pack : packageList) {
                addPackage(pack);
            }
        }
    }

    public void addPackage(final String pack) {
        if (!CabinStringUtil.isBlank(pack)) {
            importPackageList.add(pack.trim());
        }
    }

    public boolean isLoadFromBizClassLoader() {
        return loadFromBizClassLoader;
    }

    public void setLoadFromBizClassLoader(final boolean loadFromBizClassLoader) {
        this.loadFromBizClassLoader = loadFromBizClassLoader;
    }

    public List<String> getImportClassList() {
        return importClassList;
    }

    public List<String> getImportPackageList() {
        return importPackageList;
    }

    public boolean isLoadFromSystemClassLoader() {
        return loadFromSystemClassLoader;
    }

    public void setLoadFromSystemClassLoader(final boolean loadFromSystemClassLoader) {
        this.loadFromSystemClassLoader = loadFromSystemClassLoader;
    }

    public List<String> getImportResources() {
        return importResources;
    }

    public void addImportResources(final List<String> importResources) {
        if (importResources != null) {
            this.importResources.addAll(importResources);
        }
    }
}

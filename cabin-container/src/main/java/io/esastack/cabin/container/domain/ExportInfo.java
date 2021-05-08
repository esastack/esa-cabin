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

public class ExportInfo {

    private final List<String> jarList = new ArrayList<>();

    private final List<String> packageList = new ArrayList<>();

    private final List<String> classesList = new ArrayList<>();

    public void addJars(final List<String> jarList) {
        if (jarList != null) {
            for (String pack : jarList) {
                addJar(pack);
            }
        }
    }

    public void addJar(final String jar) {
        if (!CabinStringUtil.isBlank(jar)) {
            getJars().add(jar);
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
            getPackages().add(pack);
        }
    }

    public void addClasses(final List<String> classList) {
        if (classList != null) {
            for (String clazz : classList) {
                addClass(clazz);
            }
        }
    }

    public void addClass(final String clazz) {
        if (!CabinStringUtil.isBlank(clazz)) {
            getClasses().add(clazz);
        }
    }

    public List<String> getJars() {
        return jarList;
    }

    public List<String> getPackages() {
        return packageList;
    }

    public List<String> getClasses() {
        return classesList;
    }

}

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
package io.esastack.cabin.module.mojo;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;

public class PropertiesConfig {

    public static final String VALUE_SPLIT = ",";

    /**
     * imported or exported packages config
     */
    private LinkedHashSet<String> packages = new LinkedHashSet<>();

    /**
     * imported or exported classes config
     */
    private LinkedHashSet<String> classes = new LinkedHashSet<>();

    /**
     * imported or exported class config
     */
    private LinkedHashSet<String> resources = new LinkedHashSet<>();

    public String getPackagesString() {
        return StringUtils.join(packages.iterator(), VALUE_SPLIT);
    }

    public LinkedHashSet<String> getPackages() {
        return packages;
    }

    public void setPackages(LinkedHashSet<String> packages) {
        this.packages = packages;
    }

    public String getClassesString() {
        return StringUtils.join(classes.iterator(), VALUE_SPLIT);
    }

    public LinkedHashSet<String> getClasses() {
        return classes;
    }

    public void setClasses(LinkedHashSet<String> classes) {
        this.classes = classes;
    }

    public String getResourcesString() {
        return StringUtils.join(resources.iterator(), VALUE_SPLIT);
    }

    public LinkedHashSet<String> getResources() {
        return resources;
    }

    public void setResources(LinkedHashSet<String> resources) {
        this.resources = resources;
    }

}

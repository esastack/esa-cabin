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

import io.esastack.cabin.common.CompoundEnumeration;
import io.esastack.cabin.common.exception.CabinLoaderException;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class BizModuleClassLoader extends AbstractClassLoader {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(BizModuleClassLoader.class);

    public BizModuleClassLoader(final URL[] urls) {
        this("BizModule", urls);
    }

    protected BizModuleClassLoader(final String moduleName, final URL[] urls) {
        super(moduleName, urls);
    }

    @Override
    protected Class<?> loadClass0(final String name, final boolean resolve) throws CabinLoaderException {

        //load classed which has been loaded by this classloader, the relationship is recorded by jvm
        Class<?> clazz = loadFromRecords(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadFromRecords", name);
            return clazz;
        }

        //load jdk classes
        clazz = loadJdkClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadJdkClass", name);
            return clazz;
        }

        //load cabin classed, such as spi, etc.
        clazz = loadCabinClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadCabinClass", name);
            return clazz;
        }

        //load shared classes
        clazz = loadSharedClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadSharedClass", name);
            return clazz;
        }

        //load local classes from classpath urls
        clazz = loadLocalClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadLocalClass", name);
            return clazz;
        }

        //load agent classes
        clazz = loadAgentClass(name);

        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadAgentClass", name);
            return clazz;
        }
        throw new CabinLoaderException(String.format("Could not load class {%s} from Biz", name));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Enumeration<URL> getResources0(final String name) throws IOException {
        final List<Enumeration<URL>> enumerations = new ArrayList<>();

        enumerations.add(getJdkResources(name));

        enumerations.add(getLocalResources(name));

        enumerations.add(getExportResources(name));

        enumerations.add(getJavaAgentResources(name));

        return new CompoundEnumeration<>(enumerations.toArray(new Enumeration[0]));
    }

    @Override
    protected URL getResource0(final String name) {
        URL url = getJdkResource(name);

        if (url == null) {
            url = getLocalResource(name);
        }

        if (url == null) {
            url = getExportResource(name);
        }

        if (url == null) {
            url = getJavaAgentResource(name);
        }

        return url;
    }
}

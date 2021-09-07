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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * By default, java agent jar URLs will be included in the SystemClassLoader classpath, if the APP is launched by
 * CabinClassPathLauncher, the BizModuleClassLoader can load agent classes in local biz urls;
 * But, if the APP is launched in a fat jar, the BizModuleClassLoader can not load agent classes in local biz urls;
 * And, for LibModuleClassLoader, it could not be loaded by local lib urls.
 * To make all the agent classes loaded by one unique ClassLoader, we filtered the 'Java Agent URLs' from biz URLs,
 * And create a new type of ClassLoader to load them.
 */
public class JavaAgentClassLoader extends AbstractClassLoader {

    private final URL agentUrl;

    public JavaAgentClassLoader(final URL agentUrl) {
        super("Agent Module: " + agentUrl, new URL[]{agentUrl});
        this.agentUrl = agentUrl;
    }

    public URL getAgentUrl() {
        return agentUrl;
    }

    /**
     * Why we need to load class from Biz and Libs?
     * Because the agent implementation may dependency some provided jars which is provided by users;
     * In normal setup way, these jars will loaded by AppClassloader, same as agent jars.
     */
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

        /*
         * load import classes from biz classloader, some classes such as Spring/Jedis/SPI/Log4j/Logback/Slf4j, etc,
         * should always be loaded from Biz first
         */
        clazz = loadImportClassFromBiz(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadImportClassFromBiz", name);
            return clazz;
        }

        //load local classes from classpath urls
        clazz = loadLocalClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadLocalClass", name);
            return clazz;
        }

        //load shared classes
        clazz = loadSharedClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadSharedClass", name);
            return clazz;
        }

        /*
         * Load classes from biz
         */
        clazz = loadBizClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadExternalBizClass", name);
            return clazz;
        }

        throw new CabinLoaderException(String.format("Could not load class {%s} from Agent: ", agentUrl));
    }

    @Override
    protected Enumeration<URL> getResources0(final String name) throws IOException {
        final List<Enumeration<URL>> enumerations = new ArrayList<>();

        enumerations.add(getJdkResources(name));

        enumerations.add(getLocalResources(name));

        enumerations.add(getExportResources(name));

        enumerations.add(getBizResources(name));

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
            url = getBizResource(name);
        }

        return url;
    }

}

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
 * When the agent classes are loaded?
 * while jvm define a class, it will check if any agents are provided to instrument the class, and the enhanced class
 * byte code are used to create the new Class Object with the original classloader, while the weaved-in code is
 * executed, the agent classes are load by the original classloader.
 * Default handling of JVM?
 * By default, java agent jar URLs will be included in the SystemClassLoader classpath, if cabin is not used, no matter
 * using java -jar or java -cp to setup a program, all the agent urls and biz urls are in the SystemClassloader.
 * Handling of Cabin?
 * + While cabin is used, agent urls can be handled as biz urls, and there is no isolation between agent and biz urls.
 * + Cabin use JavaAgentClassLoader to load agent classes for isolation.
 * + Agent classes can load classes from biz: default imported classes(spring/jedis/slf4j, etc.), provided classes.
 * + Agent Classes can load classes from lib: exported classes, provided classes.
 * + Biz and lib can load classes from agent: classes used in weaved-in code.
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
     * The Pre-Main class would be loaded by SystemClassLoader while the Jvm started;
     * If some code are weaved in, the code may include another Class in the agent jar and this class may depend on
     * some biz/module classes, this class would be loaded by agent classloader, so agent classloader must delegate
     * class loading to biz and module classloader too;
     * Because the agent implementation may dependency some provided jars which is provided by users;
     * In normal setup way, these jars will loaded by AppClassloader, same classloader of agent jars.
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

        //load import classes from biz classloader, some classes such as Spring/Jedis/SPI/Log4j/Logback/Slf4j, etc,
        //should always be loaded from Biz first
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

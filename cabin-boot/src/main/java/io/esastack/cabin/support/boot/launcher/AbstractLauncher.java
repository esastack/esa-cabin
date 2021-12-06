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
package io.esastack.cabin.support.boot.launcher;

import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.jar.JarFile;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.esastack.cabin.common.constant.Constants.*;

/**
 * This Abstract class use the URL of cabin jars、biz、lib modules to create and start the container;
 * Implementations should collect the URLs from Fat Jar or classpath;
 * <p>
 * Classes of cabin-common/cabin-archive/cabin-boot would be shaded to the top directory of executable biz jar for
 * starting the fat jar by java -jar, so these modules should not depend on third part lib, or else
 * ClassNotFoundException would be thrown at Runtime.
 * <p>
 * Only premain java agents are supported for isolation at current version.
 */
public abstract class AbstractLauncher {

    protected final URL[] agentUrls;

    protected String[] arguments;

    protected AbstractLauncher() {
        this.agentUrls = ClassLoaderUtils.getAgentClassPath();
    }

    public Object launch(final String[] args) throws Exception {

        //set fat jar url handler, using the custom URLStreamHandler to handle the fatjar nested URLs.
        //The cabin core artifact is a fatjar no matter in classpath or fatjar setup, so the Handler is registered
        // at the beginning of the program.
        JarFile.registerUrlProtocolHandler();

        //merge arguments
        arguments = mergeArgs(findAppMainClass(), findAppMainMethod(), args);

        /*
         * set if lazy load exported classes;
         * not use commons util because ClassNotFoundException will be thrown while setup in fat jar mode.
         */
        final String prop = System.getProperty(LAZY_LOAD_EXPORTED_CLASSES_ENABLED);
        if (prop == null || prop.trim().length() == 0) {
            System.setProperty(LAZY_LOAD_EXPORTED_CLASSES_ENABLED, Boolean.toString(lazyLoadExportClass()));
        }

        //find cabin-core jar archive, and setup cabin container
        final Archive containerArchive = findContainerArchive();
        final ClassLoader cabinClassLoader = createCabinClassLoader(containerArchive);
        final Class<?> cabinContainerClass = cabinClassLoader.loadClass(CABIN_CONTAINER_CLASSNAME);

        final ClassLoader oldTCCL = ClassLoaderUtils.pushTCCL(cabinClassLoader);
        try {
            final Object cabinContainer = createCabinContainer(cabinContainerClass);
            invokeStart(cabinContainer);
            return cabinContainer;
        } finally {
            ClassLoaderUtils.setTCCL(oldTCCL);
        }
    }

    private String[] mergeArgs(final String mainClass, final String mainMethod, final String[] args) {
        final String[] arguments = new String[args.length + 2];
        arguments[0] = mainClass;
        arguments[1] = mainMethod;
        System.arraycopy(args, 0, arguments, 2, args.length);
        return arguments;
    }

    /**
     * User the cabin-core artifact to create cabin container classloader, it's a fatjar with conf and libs directories.
     * @param containerArchive for classpath setup, it's a normal jar file; for fatjar setup, it's a nest jar file.
     * @return CabinContainerClassloader
     * @throws Exception exception
     */
    private ClassLoader createCabinClassLoader(final Archive containerArchive) throws Exception {
        final List<Archive> containerClasspath = containerArchive.getNestedArchives(entry ->
                (entry.isDirectory() && entry.getName().endsWith(NESTED_CONF_DIRECTORY)) ||
                        (!entry.isDirectory() && entry.getName().startsWith(NESTED_LIB_DIRECTORY) &&
                                entry.getName().endsWith(JAR_FILE_SUFFIX)));
        final List<URL> urls = new ArrayList<>(containerClasspath.size());
        for (Archive archive : containerClasspath) {
            urls.add(archive.getUrl());
        }
        return new CabinContainerClassLoader(urls.toArray(new URL[0]));
    }

    protected void invokeStart(final Object container) {
        try {
            final Method method = container.getClass().getDeclaredMethod("start");
            method.invoke(container);
        } catch (Throwable t) {
            throw new CabinRuntimeException("Failed to start Cabin container", t);
        }
    }

    protected boolean lazyLoadExportClass() {
        return false;
    }

    protected String findAppMainMethod() {
        return "main";
    }

    protected String[] findJavaAgentUrls() {
        final String[] urls = new String[agentUrls.length];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = agentUrls[i].toExternalForm();
        }
        return urls;
    }

    protected abstract Object createCabinContainer(final Class<?> cabinContainerClass) throws Exception;

    protected abstract String findAppMainClass() throws Exception;

    protected abstract Archive findContainerArchive() throws Exception;

}

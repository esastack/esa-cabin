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
package io.esastack.cabin.container;

import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.api.service.share.SharedClassService;
import io.esastack.cabin.common.constant.Constants;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.domain.LibModule;
import io.esastack.cabin.container.initialize.CabinBootContext;
import io.esastack.cabin.container.initialize.Initializer;
import io.esastack.cabin.container.service.CabinServiceManager;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.util.ArchiveUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The entrance of starting cabin container, core class;
 * Deploy modules, export shared classes, scan spi implementations, etc.
 * Biz main class will be executed after all bizModule and modules have been loaded.
 */
public class CabinContainer {

    private static final String PATH = "loader.path";

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(CabinContainer.class);

    private final CabinBootContext cabinBootContext;

    private final AtomicBoolean started = new AtomicBoolean();

    private final long startAt = System.currentTimeMillis();

    /**
     * instanced by bootstrap, launcher, Test runner;
     * start the container services pipeline to deploy modules, start services;
     * Should not pass any Object except primitives and String, because any Object like URL/JarFile would bring
     * SystemClassLoader with it; for example, 'URL' will bring a Handler loaded by SystemClassLoader which would load
     * other classes with SystemClassLoader; so we should not pass these Object, and make sure SystemClassLoader is
     * no longer used. An example for this is: URL to Handler to openConnection, in this method,JarURLConnection would
     * be loaded by SystemClassloader and JarURLConnection::useFastExceptions would always be false Because Handler::
     * setUseFastConnectionExceptions would set another JarURLConnection Class loaded by CabinClassLoader.
     * <p>
     * this constructor is used while launching the application in classpath
     *
     * @param containerURLString  the cabin core fat jar file path
     * @param moduleURLStrings    the lib module fat jar file paths
     * @param bizURLStrings       the biz classpath paths
     * @param javaAgentURLStrings the setup java agent jar file paths
     * @param args                set up main args
     */
    public CabinContainer(final String containerURLString,
                          final String[] moduleURLStrings,
                          final String[] bizURLStrings,
                          final String[] javaAgentURLStrings,
                          final String[] args) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cabin setup in classpath mode!");
            LOGGER.debug(CabinStringUtil.mergeStringArray("Cabin container url:", containerURLString));
            LOGGER.debug(CabinStringUtil.mergeStringArray("Cabin module urls:", moduleURLStrings));
            LOGGER.debug(CabinStringUtil.mergeStringArray("Cabin biz urls:", bizURLStrings));
        }

        final Archive containerArchive;
        try {
            containerArchive = ArchiveUtils.createArchiveFromUrl(convertString2URL(containerURLString));
        } catch (Throwable ex) {
            throw new CabinRuntimeException("Failed to create Cabin archive from url", ex);
        }
        this.cabinBootContext = CabinBootContext.newBuilder()
                .cabinContainer(this)
                .containerArchive(containerArchive)
                .moduleUrls(convertString2URLs(moduleURLStrings))
                .bizUrls(mergeBizUrlWithLoaderPath(convertString2URLs(bizURLStrings)))
                .javaAgentUrls(convertString2URLs(javaAgentURLStrings))
                .arguments(args)
                .build();
    }

    /**
     * Same as previous constructor, except this is used while launching the application in fat jar;
     * Create the biz URLs in CabinContainer, so the handler of the URL would bound to a JarFile, this will make the
     * biz class loading faster;
     *
     * @param executableURLString the application executable fat jar
     * @param javaAgentURLStrings the setup java agent jar file paths
     * @param args                set up main args
     */
    public CabinContainer(final String executableURLString,
                          final String[] javaAgentURLStrings,
                          final String[] args) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cabin setup in executable jar mode!");
            LOGGER.debug(CabinStringUtil.mergeStringArray("Cabin executable url:", executableURLString));
        }

        final Archive executableArchive;
        try {
            executableArchive = ArchiveUtils.createArchiveFromUrl(new URL(executableURLString));
        } catch (IOException e) {
            throw new CabinRuntimeException("Failed to create Cabin executable archive from url", e);
        }

        this.cabinBootContext = CabinBootContext.newBuilder()
                .cabinContainer(this)
                .containerArchive(ArchiveUtils.extractContainerArchive(executableArchive))
                .moduleUrls(ArchiveUtils.extractModuleURLs(executableArchive))
                .bizUrls(mergeBizUrlWithLoaderPath(ArchiveUtils.extractBizUrls(executableArchive)))
                .javaAgentUrls(convertString2URLs(javaAgentURLStrings))
                .arguments(args)
                .build();
    }

    public void start() {
        if (this.started.compareAndSet(false, true)) {
            final long initStartTime = System.currentTimeMillis();
            CabinServiceManager.get().init();
            LOGGER.info("CabinServiceManager init cost: " + (System.currentTimeMillis() - initStartTime) + "ms");
            final Initializer initializer = CabinServiceManager.get().getService(Initializer.class);
            initializer.initialize(this.cabinBootContext);
            final String setUpMsg = "Cabin Container started in " + (System.currentTimeMillis() - startAt) + " ms.";
            LOGGER.info(setUpMsg);
            System.out.println(setUpMsg);
        }
    }

    public void stop() {
        CabinServiceManager.get().destroy();
    }

    public boolean isStarted() {
        return this.started.get();
    }

    public Map<String, Class<?>> getExportedClasses() {
        final SharedClassService sharedClassService = CabinServiceManager.get().getService(SharedClassService.class);
        final Map<String, Class<?>> sharedClassMap = sharedClassService.getSharedClassMap();
        if (sharedClassMap == null || sharedClassMap.isEmpty()) {
            LOGGER.error("There is no class exported by Cabin Container, please check the module dependencies");
        }
        return sharedClassMap;
    }

    public boolean moduleLoaded(final String moduleName) {
        if (CabinStringUtil.isBlank(moduleName)) {
            return false;
        }
        final LibModuleLoadService libModuleLoadService =
                CabinServiceManager.get().getService(LibModuleLoadService.class);
        if (libModuleLoadService == null) {
            return false;
        }
        return libModuleLoadService.getModule(moduleName) != null;
    }

    public List<String> getLoadedModule() {
        final List<String> modules = new ArrayList<>();
        final LibModuleLoadService libModuleLoadService =
                CabinServiceManager.get().getService(LibModuleLoadService.class);
        if (libModuleLoadService != null) {
            libModuleLoadService.getAllModules().forEach(module -> modules.add(module.getName()));
        }
        return modules;
    }

    public ClassLoader getLibModuleClassLoader(final String moduleName) {
        final LibModuleLoadService libModuleLoadService =
                CabinServiceManager.get().getService(LibModuleLoadService.class);
        if (libModuleLoadService == null) {
            return null;
        }
        final LibModule module = (LibModule) libModuleLoadService.getModule(moduleName);
        if (module == null) {
            return null;
        }
        return module.getClassLoader();
    }

    public ClassLoader getBizModuleClassLoader() {
        final ClassLoaderService classLoaderService = CabinServiceManager.get().getService(ClassLoaderService.class);
        if (classLoaderService == null) {
            return null;
        }
        return classLoaderService.getBizModuleClassLoader();
    }

    private URL[] convertString2URLs(final String[] args) {
        if (args == null || args.length == 0) {
            return new URL[0];
        }
        final URL[] urls = new URL[args.length];
        for (int i = 0; i < args.length; i++) {
            urls[i] = convertString2URL(args[i]);
        }
        return urls;
    }

    private URL convertString2URL(final String arg) {
        try {
            return new URL(arg);
        } catch (MalformedURLException e) {
            throw new CabinRuntimeException(String.format("Failed to convert arg %s to URL", arg), e);
        }
    }

    private URL[] mergeBizUrlWithLoaderPath(final URL[] bizUrls) {
        final String loaderPath = System.getProperty(PATH);
        if (CabinStringUtil.isBlank(loaderPath)) {
            return bizUrls;
        }

        final List<URL> pathUrls = new ArrayList<>();
        final String[] paths = loaderPath.split(",");
        for (String path : paths) {
            path = path.trim();
            if (CabinStringUtil.isBlank(path)) {
                continue;
            }

            final File file = new File(path);
            if (!file.exists()) {
                throw new CabinRuntimeException(
                        String.format("-Dloader.path contains path that does not exist: %s!", path));
            }
            if (!file.isDirectory()) {
                throw new CabinRuntimeException(
                        String.format("-Dloader.path contains path that is not directory: %s!", path));
            }

            final URL pathUrl;
            try {
                pathUrl = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new CabinRuntimeException(
                        String.format("-Dloader.path contains invalid path that cannot convert to URL: %s!", path));
            }
            pathUrls.add(pathUrl);
        }

        if (pathUrls.isEmpty()) {
            return bizUrls;
        }

        //Make sure the paths configured by "-Dloader.path" are in the front of biz urls, that means these urls would
        //be loaded prior to biz urls.
        final URL[] mergedUrls = new URL[bizUrls.length + pathUrls.size()];
        System.arraycopy(pathUrls.toArray(new URL[0]), 0, mergedUrls, 0, pathUrls.size());
        System.arraycopy(bizUrls, 0, mergedUrls, pathUrls.size(), bizUrls.length);

        return mergedUrls;
    }
}

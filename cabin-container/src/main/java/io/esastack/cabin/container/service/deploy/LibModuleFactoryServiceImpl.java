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
package io.esastack.cabin.container.service.deploy;

import io.esastack.cabin.api.service.deploy.LibModuleFactoryService;
import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.api.service.loader.LibModuleClassLoaderParam;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.domain.ExportInfo;
import io.esastack.cabin.container.domain.ImportInfo;
import io.esastack.cabin.container.domain.LibModule;
import io.esastack.cabin.loader.archive.Archive;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.esastack.cabin.common.constant.Constants.*;

public class LibModuleFactoryServiceImpl implements LibModuleFactoryService<LibModule> {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleFactoryServiceImpl.class);

    private volatile ClassLoaderService classLoaderService;

    @SuppressWarnings("unused")
    public void setClassLoaderService(final ClassLoaderService service) {
        classLoaderService = service;
    }

    /**
     * Only put itself, libs/ and conf/ dir into classpath
     */
    private List<URL> getLibModuleClasspathUrls(final Archive archive) throws IOException {
        final List<URL> result = new ArrayList<>();
        final List<Archive> nestedJarArchives = archive.getNestedArchives((entry) ->
                (entry.isDirectory() && entry.getName().endsWith(NESTED_CONF_DIRECTORY)) ||
                        (!entry.isDirectory() && entry.getName().startsWith(NESTED_LIB_DIRECTORY) &&
                                entry.getName().endsWith(JAR_FILE_SUFFIX)));
        for (Archive a : nestedJarArchives) {
            result.add(a.getUrl());
        }
        result.add(archive.getUrl());
        return result;
    }

    @Override
    public LibModule createModule(final String name, final Archive archive) throws CabinRuntimeException {

        final int priority = getPriority(name, archive);
        final ExportInfo exportInfo = getExportInfo(name, archive);
        final ImportInfo importInfo = getImportInfo(name, archive);
        final List<String> providedClasses = getProvidedClasses(name, archive);
        final URL[] urls;
        try {
            urls = getLibModuleClasspathUrls(archive).toArray(new URL[0]);
        } catch (IOException e) {
            throw new CabinRuntimeException(String.format("Failed to get jar from libs of module {%s}", name));
        }

        final LibModuleClassLoaderParam param = LibModuleClassLoaderParam.newBuilder()
                .moduleName(name)
                .urls(urls)
                .providedClassList(providedClasses)
                .importClassList(importInfo.getImportClassList())
                .importPackageList(importInfo.getImportPackageList())
                .importResources(importInfo.getImportResources())
                .loadFromBizClassLoader(importInfo.isLoadFromBizClassLoader())
                .loadFromSystemClassLoader(importInfo.isLoadFromSystemClassLoader())
                .build();
        final ClassLoader classLoader = classLoaderService.createLibModuleClassLoader(param);
        return LibModule.newBuilder()
                .name(name)
                .priority(priority)
                .archive(archive)
                .exportInfo(exportInfo)
                .importInfo(importInfo)
                .providedClasses(providedClasses)
                .classLoader(classLoader)
                .build();
    }

    @Override
    public void destroyModule(String name) throws CabinRuntimeException {
        classLoaderService.destroyLibModuleClassLoader(name);
    }

    private int getPriority(final String name, final Archive archive) {
        try {
            final String priority = archive.getManifest().getMainAttributes().getValue(MANIFEST_MODULE_PRIORITY);
            if (CabinStringUtil.isNotBlank(priority)) {
                return Integer.parseInt(priority);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get priority from manifest of module archive: " + name, e);
        }
        return 100;
    }

    private String getVersion(final String name, final Archive archive) {
        try {
            return archive.getManifest().getMainAttributes().getValue(MANIFEST_MODULE_VERSION);
        } catch (IOException e) {
            LOGGER.error("Failed to get version from manifest of module archive: " + name, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getProvidedClasses(final String name, final Archive archive) {
        final List<String> classes = new ArrayList<>();
        final URL providedClassFile = archive.getResource(PROVIDED_CLASS_FILE);
        if (providedClassFile == null) {
            LOGGER.warn("Failed to find conf/provided_classes file from archive of module: " + name);
            return Collections.EMPTY_LIST;
        }

        try {
            final BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(providedClassFile.openStream()));
            String clazz;
            while ((clazz = bufferedReader.readLine()) != null) {
                if (!CabinStringUtil.isBlank(clazz)) {
                    clazz = clazz.trim();
                    classes.add(clazz);
                }
            }
            return classes;
        } catch (IOException e) {
            throw new CabinRuntimeException(
                    "Failed open conf/provided_classes file from archive of module: " + name, e);
        }
    }

    private ExportInfo getExportInfo(final String name, final Archive archive) {
        final ExportInfo exportInfo = new ExportInfo();
        try {
            final String exportClasses = archive.getManifest().getMainAttributes().getValue(MANIFEST_EXPORT_CLASSES);
            if (!CabinStringUtil.isBlank(exportClasses)) {
                exportInfo.addClasses(Arrays.asList(exportClasses.split(",")));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get Export-Classes from manifest of module archive: " + name, e);
        }

        try {
            final String exportPackages = archive.getManifest().getMainAttributes().getValue(MANIFEST_EXPORT_PACKAGES);
            if (!CabinStringUtil.isBlank(exportPackages)) {
                exportInfo.addPackages(Arrays.asList(exportPackages.split(",")));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get Export-Packages from manifest of module archive: " + name, e);
        }

        try {
            final String exportJars = archive.getManifest().getMainAttributes().getValue(MANIFEST_EXPORT_JARS);
            if (!CabinStringUtil.isBlank(exportJars)) {
                exportInfo.addJars(Arrays.asList(exportJars.split(",")));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get Export-Jars from manifest of module archive: " + name, e);
        }

        return exportInfo;
    }

    private ImportInfo getImportInfo(final String name, final Archive archive) {
        final ImportInfo importInfo = new ImportInfo();
        try {
            final String importClasses = archive.getManifest().getMainAttributes().getValue(MANIFEST_IMPORT_CLASSES);
            if (!CabinStringUtil.isBlank(importClasses)) {
                importInfo.addClasses(Arrays.asList(importClasses.split(",")));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get Import-Packages from manifest of module archive: " + name, e);
        }

        try {
            final String importPackages = archive.getManifest().getMainAttributes().getValue(MANIFEST_IMPORT_PACKAGES);
            if (!CabinStringUtil.isBlank(importPackages)) {
                importInfo.addPackages(Arrays.asList(importPackages.split(",")));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get Import-Packages from manifest of module archive: " + name, e);
        }

        try {
            final String importResources =
                    archive.getManifest().getMainAttributes().getValue(MANIFEST_IMPORT_RESOURCES);
            if (!CabinStringUtil.isBlank(importResources)) {
                importInfo.addImportResources(Arrays.asList(importResources.split(",")));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get Import-Resources from manifest of module archive: " + name, e);
        }

        try {
            final String loadFromBizClassLoader =
                    archive.getManifest().getMainAttributes().getValue(MANIFEST_LOAD_FROM_BIZ);
            if (!CabinStringUtil.isBlank(loadFromBizClassLoader)) {
                importInfo.setLoadFromBizClassLoader(Boolean.parseBoolean(loadFromBizClassLoader));
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to get LoadFromBizClassLoader from manifest of module archive: " + name, e);
        }

        try {
            importInfo.setLoadFromSystemClassLoader(Boolean.getBoolean("loadFromSystemClassLoader"));
        } catch (Throwable e) {
            LOGGER.error("Failed to parse oadFromSystemClassLoader from system properties", e);
        }

        return importInfo;
    }
}

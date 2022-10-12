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
package io.esastack.cabin.container.processor;

import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.initialize.CabinBootContext;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.util.ArchiveUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.esastack.cabin.common.constant.Constants.*;

/**
 * convert module URL to module Archive;
 * merge modules contained in Container Archive if any;
 * merge modules contained in modules recursively if any;
 * Priority: Container contained modules , biz modules , recursively contained modules;
 * if two modules both contains another module, if the version is the same, use it, or you should put the module in the
 * biz classpath to avoid conflicting
 */
public class LibModuleMergeProcessor implements Processor {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleMergeProcessor.class);

    private static final Boolean ignoreDuplicatedNestModule =
            Boolean.parseBoolean(System.getProperty(CABIN_DUPLICATED_MODULE_IGNORE, "true"));

    public void process(final CabinBootContext cabinBootContext) throws CabinRuntimeException {
        try {
            final Map<String, Archive> externalModules = parseLibModulesFromExternalDir();
            final Map<String, Archive> containerModules =
                    parseLibModulesFromContainerArchive(cabinBootContext.getContainerArchive());
            final Map<String, Archive> modules = parseLibModulesFromURLs(
                    cabinBootContext.getModuleUrls(), false, false);
            //module prioritized: external modules > nest modules > user dependencies
            modules.putAll(containerModules);
            modules.putAll(externalModules);
            cabinBootContext.setModuleArchives(parseNestLibModulesRecursively(modules));
        } catch (Throwable e) {
            throw new CabinRuntimeException(e.getMessage(), e);
        }
    }

    public Map<String, Archive> parseLibModulesFromExternalDir() {
        final String dir = System.getProperty(CABIN_MODULE_DIR, CABIN_MODULE_DIR_DEFAULT);
        if (CabinStringUtil.isNotBlank(dir)) {
            final File file = new File(dir);
            if (file.exists() && file.isDirectory()) {
                final File[] nestFiles = file.listFiles();
                if (nestFiles != null) {
                    final List<URL> urls = new ArrayList<>(nestFiles.length);
                    for (File f: nestFiles) {
                        if (f.isFile()) {
                            try {
                                urls.add(f.toURI().toURL());
                            } catch (MalformedURLException e) {
                                LOGGER.warn("Invalid file configured in dir!", e);
                            }
                        }
                    }
                    if (urls.size() != 0) {
                        try {
                            return parseLibModulesFromURLs(urls.toArray(new URL[0]), true, false);
                        } catch (IOException e) {
                            // never common here.
                        }
                    }
                }
            }
        }
        return new HashMap<>();
    }

    private Map<String, Archive> parseLibModulesFromContainerArchive(final Archive containerArchive)
            throws IOException {
        final List<Archive> nestModules = containerArchive.getNestedArchives(entry -> !entry.isDirectory() &&
                entry.getName().startsWith(NESTED_MODULE_DIRECTORY) && entry.getName().endsWith(JAR_FILE_SUFFIX));
        final List<Archive> containerArchives = containerArchive.getNestedArchives(entry ->
                (entry.isDirectory() && entry.getName().endsWith(NESTED_CONF_DIRECTORY)) ||
                        (!entry.isDirectory() && entry.getName().startsWith(NESTED_LIB_DIRECTORY) &&
                                entry.getName().endsWith(JAR_FILE_SUFFIX)));

        if (LOGGER.isDebugEnabled()) {
            final URL[] urls = new URL[containerArchives.size()];
            for (int i = 0; i < urls.length; i++) {
                urls[i] = containerArchives.get(i).getUrl();
            }

            final URL[] moduleUrls = new URL[nestModules.size()];
            for (int i = 0; i < moduleUrls.length; i++) {
                moduleUrls[i] = nestModules.get(i).getUrl();
            }
            LOGGER.debug(CabinStringUtil.urlsToString("Cabin container classpath:", urls));
            LOGGER.debug(CabinStringUtil.urlsToString("Cabin container nested modules:", moduleUrls));
        }

        final Map<String, Archive> containerModules = new HashMap<>();
        for (Archive nestModule : nestModules) {
            final String moduleName = nestModule.getManifest().getMainAttributes().getValue(MANIFEST_MODULE_NAME);
            if (CabinStringUtil.isBlank(moduleName)) {
                throw new CabinRuntimeException("Invalid module Manifest, blank Module-Name, "
                        + nestModule.getUrl().toExternalForm());
            }
            if (containerModules.put(moduleName, nestModule) != null) {
                throw new CabinRuntimeException(
                        String.format("Duplicated module {%s} found in Cabin container archive", moduleName));
            }
        }
        return containerModules;
    }

    public Map<String, Archive> parseLibModulesFromURLs(
            final URL[] moduleUrls, final boolean ignoreException, final boolean recursively) throws IOException {
        final Map<String, Archive> urlModules = new HashMap<>();
        if (moduleUrls != null) {
            for (URL moduleUrl : moduleUrls) {
                try {
                    final Archive module = ArchiveUtils.createArchiveFromUrl(moduleUrl);
                    final String moduleName = module.getManifest().getMainAttributes().getValue(MANIFEST_MODULE_NAME);
                    if (CabinStringUtil.isBlank(moduleName)) {
                        throw new CabinRuntimeException("Invalid module Manifest, blank Module-Name, "
                                + moduleUrl.toExternalForm());
                    }
                    if (urlModules.put(moduleName, module) != null) {
                        throw new CabinRuntimeException(
                                String.format("Duplicated module {%s} found in biz urls", moduleName));
                    }
                } catch (Throwable e) {
                    if (ignoreException) {
                        LOGGER.warn("Failed to parse lib module from " + moduleUrl.toExternalForm());
                    } else {
                        throw e;
                    }
                }
            }
        }
        if (recursively) {
            return parseNestLibModulesRecursively(urlModules);
        }
        return urlModules;
    }

    private Map<String, Archive> parseNestLibModulesRecursively(final Map<String, Archive> modules) throws IOException {
        final Map<String, Archive> mergedModules = new HashMap<>();
        Map<String, Archive> modules4Check = modules;
        while (modules4Check.size() > 0) {
            final Map<String, Archive> nestedModules = new HashMap<>();
            for (Map.Entry<String, Archive> entry : modules4Check.entrySet()) {
                final Archive archive = entry.getValue();
                final List<Archive> nestArchives = archive.getNestedArchives(a -> !a.isDirectory() &&
                        a.getName().startsWith(NESTED_MODULE_DIRECTORY) && a.getName().endsWith(JAR_FILE_SUFFIX));
                for (Archive nestArchive : nestArchives) {
                    final String moduleName =
                            nestArchive.getManifest().getMainAttributes().getValue(MANIFEST_MODULE_NAME);
                    if (CabinStringUtil.isBlank(moduleName)) {
                        throw new CabinRuntimeException("Invalid module Manifest, blank Module-Name, "
                                + nestArchive.getUrl().toExternalForm());
                    }
                    if (modules.containsKey(moduleName)) {
                        LOGGER.info("Duplicated module named {} found, URL {}(imported by biz url) would be used," +
                                        " {} is ignored!",
                                moduleName,
                                modules.get(moduleName).getUrl().toExternalForm(),
                                nestArchive.getUrl().toExternalForm());
                    } else if (mergedModules.containsKey(moduleName)) {
                        final String errorMsg = String.format("Duplicated nested module named {%s} found, " +
                                        "URL {%s} would be used, {%s} is ignored! if you want resolve this problem," +
                                        " put this cabin module in your biz dependencies.",
                                moduleName,
                                mergedModules.get(moduleName).getUrl().toExternalForm(),
                                nestArchive.getUrl().toExternalForm());
                        if (!ignoreDuplicatedNestModule) {
                            throw new CabinRuntimeException(errorMsg);
                        }
                        LOGGER.warn(errorMsg);
                    } else {
                        nestedModules.put(moduleName, nestArchive);
                        mergedModules.put(moduleName, nestArchive);
                    }
                }
            }
            modules4Check = nestedModules;
        }
        mergedModules.putAll(modules);
        return mergedModules;
    }
}


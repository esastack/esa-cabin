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
package io.esastack.cabin.container.service.share;

import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.api.service.share.LibModuleExportService;
import io.esastack.cabin.api.service.share.SharedClassService;
import io.esastack.cabin.api.service.share.SharedResourceService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.domain.LibModule;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.esastack.cabin.common.constant.Constants.EXPORTED_CLASS_FILE;
import static io.esastack.cabin.common.constant.Constants.EXPORTED_RESOURCE_FILE;

public class LibModuleExportServiceImpl implements LibModuleExportService {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleExportServiceImpl.class);

    private volatile SharedClassService sharedClassService;

    private volatile SharedResourceService sharedResourceService;

    private volatile LibModuleLoadService libModuleLoadService;

    public void setSharedClassService(final SharedClassService service) {
        sharedClassService = service;
    }

    public void setSharedResourceService(final SharedResourceService service) {
        sharedResourceService = service;
    }

    public void setLibModuleLoadService(final LibModuleLoadService service) {
        libModuleLoadService = service;
    }

    @Override
    public void preLoadAllSharedClasses() {
        sharedClassService.preLoadAllSharedClasses();
    }

    @Override
    public int exportResources(final String moduleName) throws CabinRuntimeException {
        if (CabinStringUtil.isBlank(moduleName)) {
            return 0;
        }

        final LibModule libModule = (LibModule) libModuleLoadService.getModule(moduleName);
        if (libModule == null) {
            LOGGER.warn(String.format("Could not find LibModule of %s", moduleName));
            return 0;
        }

        final URL exportResourcesUrl = libModule.getArchive().getResource(EXPORTED_RESOURCE_FILE);
        if (exportResourcesUrl == null) {
            LOGGER.info("Failed to find 'conf/export_resources' file from archive of module: " + moduleName);
            return -1;
        }

        int exportedCount = 0;
        final List<String> exportResources = new ArrayList<>();
        try {
            final BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(exportResourcesUrl.openStream()));
            String resource;
            while ((resource = bufferedReader.readLine()) != null) {
                if (CabinStringUtil.isNotBlank(resource)) {
                    resource = resource.trim();
                    exportResources.add(resource.trim());
                    sharedResourceService.addExportClassLoader(resource.trim(), libModule.getClassLoader());
                    exportedCount++;
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(CabinStringUtil.mergeStringArray("Module{" + moduleName + "} export resources:",
                        exportResources.toArray(new String[0])));
            }
        } catch (IOException e) {
            throw new CabinRuntimeException(
                    "Failed open conf/export_resources file from archive of module: " + moduleName, e);
        }

        return exportedCount;
    }

    @Override
    public int exportClasses(final String moduleName) throws CabinRuntimeException {
        if (CabinStringUtil.isBlank(moduleName)) {
            return 0;
        }

        final LibModule libModule = (LibModule) libModuleLoadService.getModule(moduleName);
        if (libModule == null) {
            return 0;
        }

        for (String packageName : libModule.getExportInfo().getPackages()) {
            if (CabinStringUtil.isNotBlank(packageName)) {
                sharedClassService.addSharedPackage(packageName, libModule);
            }
        }

        final URL exportedClassFile = libModule.getArchive().getResource(EXPORTED_CLASS_FILE);
        if (exportedClassFile == null) {
            LOGGER.warn("Failed to find conf/export_classes file from archive of module: " + moduleName);
            return -1;
        }

        int exportedCount = 0;
        final List<String> exportedClasses = new ArrayList<>();
        try (final BufferedReader bufferedReader =
                     new BufferedReader(new InputStreamReader(exportedClassFile.openStream()))) {
            String clazz;
            while ((clazz = bufferedReader.readLine()) != null) {
                if (CabinStringUtil.isNotBlank(clazz)) {
                    clazz = clazz.trim();
                    exportedClasses.add(clazz);
                    sharedClassService.addSharedClass(clazz, libModule);
                    exportedCount++;
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(CabinStringUtil.mergeStringArray("Module{" + moduleName + "} export classes:",
                        exportedClasses.toArray(new String[0])));
            }
        } catch (IOException e) {
            throw new CabinRuntimeException(
                    "Failed open conf/export_classes file from archive of module: " + moduleName, e);
        }

        return exportedCount;
    }

    @Override
    public void destroyModule(String moduleName) {
        sharedClassService.destroyModuleClasses(moduleName);
        sharedResourceService.destroyModuleResources(moduleName);
    }
}

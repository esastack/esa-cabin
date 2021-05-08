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

import io.esastack.cabin.api.domain.Module;
import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.api.service.share.LibModuleExportService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.container.initialize.CabinBootContext;
import io.esastack.cabin.container.service.share.LazyLoadExportDetector;
import io.esastack.cabin.container.service.share.LibModuleExportServiceImpl;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class LibModuleExportProcessor implements Processor {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleExportProcessor.class);

    private volatile LibModuleLoadService libModuleLoadService;

    private volatile LibModuleExportService libModuleExportService;

    public void process(final CabinBootContext cabinBootContext) throws CabinRuntimeException {
        final List<Module> modules = libModuleLoadService.getAllModules();
        final AtomicReference<Throwable> exportError = new AtomicReference<>();
        if (modules != null && modules.size() > 0) {
            final CountDownLatch latch = new CountDownLatch(modules.size());
            int threadIndex = 1;
            for (Module module : modules) {
                new Thread(() -> {
                    try {
                        int count = libModuleExportService.exportResources(module.getName());
                        LOGGER.info("Exported {} resources from module {}", count, module.getName());
                        count = libModuleExportService.exportClasses(module.getName());
                        LOGGER.info("Exported {} classes from module {}", count, module.getName());
                    } catch (Throwable ex) {
                        exportError.set(ex);
                        LOGGER.error("Failed to export module!", ex);
                    } finally {
                        latch.countDown();
                    }
                }, "LibModuleExportThread-" + threadIndex++).start();
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new CabinRuntimeException(e);
            }
            if (exportError.get() != null) {
                throw new CabinRuntimeException("Failed to load classes and resources from module!", exportError.get());
            }
            if (!LazyLoadExportDetector.isLazyLoad()) {
                libModuleExportService.preLoadAllSharedClasses();
            }
        }
    }

    public void setLibModuleLoadService(final LibModuleLoadService service) {
        libModuleLoadService = service;
    }

    public void setLibModuleExportService(final LibModuleExportServiceImpl service) {
        libModuleExportService = service;
    }
}

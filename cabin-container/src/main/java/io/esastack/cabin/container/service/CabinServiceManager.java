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
package io.esastack.cabin.container.service;

import io.esastack.cabin.api.service.deploy.*;
import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.api.service.share.LibModuleExportService;
import io.esastack.cabin.api.service.share.SharedClassService;
import io.esastack.cabin.api.service.share.SharedResourceService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.container.initialize.DefaultInitializer;
import io.esastack.cabin.container.initialize.Initializer;
import io.esastack.cabin.container.processor.*;
import io.esastack.cabin.container.service.deploy.*;
import io.esastack.cabin.container.service.loader.ClassLoaderServiceImpl;
import io.esastack.cabin.container.service.share.LibModuleExportServiceImpl;
import io.esastack.cabin.container.service.share.SharedClassServiceImpl;
import io.esastack.cabin.container.service.share.SharedResourceServiceImpl;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keep all cabin service used globally, IOC;
 */
public class CabinServiceManager {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(CabinServiceManager.class);

    private static final CabinServiceManager singleton = new CabinServiceManager();

    private final AtomicBoolean inited = new AtomicBoolean(false);

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final MutablePicoContainer picoContainer = new PicoBuilder().withSetterInjection().withCaching().build();

    private CabinServiceManager() {
        try {
            this.picoContainer.addComponent(JavaAgentModuleLoadService.class, JavaAgentModuleLoadServiceImpl.class);
            this.picoContainer.addComponent(BizModuleLoadService.class, BizModuleLoadServiceImpl.class);
            this.picoContainer.addComponent(LibModuleLoadService.class, LibModuleLoadServiceImpl.class);

            this.picoContainer.addComponent(JavaAgentModuleFactoryService.class,
                    JavaAgentModuleFactoryServiceImpl.class);
            this.picoContainer.addComponent(BizModuleFactoryService.class, BizModuleFactoryServiceImpl.class);
            this.picoContainer.addComponent(LibModuleFactoryService.class, LibModuleFactoryServiceImpl.class);

            this.picoContainer.addComponent(ClassLoaderService.class, ClassLoaderServiceImpl.class);
            this.picoContainer.addComponent(SharedClassService.class, SharedClassServiceImpl.class);
            this.picoContainer.addComponent(SharedResourceService.class, SharedResourceServiceImpl.class);
            this.picoContainer.addComponent(LibModuleExportService.class, LibModuleExportServiceImpl.class);
            this.picoContainer.addComponent(Initializer.class, DefaultInitializer.class);

            this.picoContainer.addComponent(JavaAgentModuleLoadProcessor.class, JavaAgentModuleLoadProcessor.class);
            this.picoContainer.addComponent(BizModuleLoadProcessor.class, BizModuleLoadProcessor.class);
            this.picoContainer.addComponent(LibModuleMergeProcessor.class, LibModuleMergeProcessor.class);
            this.picoContainer.addComponent(LibModuleLoadProcessor.class, LibModuleLoadProcessor.class);
            this.picoContainer.addComponent(LibModuleExportProcessor.class, LibModuleExportProcessor.class);
            this.picoContainer.addComponent(ContainerStateExportProcessor.class, ContainerStateExportProcessor.class);
            this.picoContainer.addComponent(BizModuleSetupProcessor.class, BizModuleSetupProcessor.class);
        } catch (Throwable t) {
            LOGGER.error("Failed to create PICO Container", t);
            throw new CabinRuntimeException("Failed to create PICO Container", t);
        }
    }

    public static CabinServiceManager get() {
        return singleton;
    }

    public void init() {
        if (inited.compareAndSet(false, true)) {
            final ClassLoader oldTCCL = ClassLoaderUtils.pushTCCL(CabinServiceManager.class.getClassLoader());
            try {
                picoContainer.start();
            } finally {
                ClassLoaderUtils.setTCCL(oldTCCL);
            }
        }
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            final ClassLoader oldTCCL = ClassLoaderUtils.pushTCCL(CabinServiceManager.class.getClassLoader());
            try {
                picoContainer.dispose();
            } finally {
                ClassLoaderUtils.setTCCL(oldTCCL);
            }
        }
    }

    public <T> T getService(final Class<T> serviceType) {
        return picoContainer.getComponent(serviceType);
    }
}

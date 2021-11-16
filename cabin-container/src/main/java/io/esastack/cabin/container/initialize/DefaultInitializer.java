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
package io.esastack.cabin.container.initialize;

import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.container.processor.*;
import io.esastack.cabin.container.service.CabinServiceManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * We have to load biz first, because the biz classloader is used to init module classloader
 */
public class DefaultInitializer implements Initializer {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(DefaultInitializer.class);

    private final List<Processor> bootProcessors = new ArrayList<>();

    /**
     * BizModuleSetupProcessor must be the last processor, because it may block the thread.
     */
    public DefaultInitializer() {
        this.bootProcessors.add(CabinServiceManager.get().getService(JavaAgentModuleLoadProcessor.class));
        this.bootProcessors.add(CabinServiceManager.get().getService(BizModuleLoadProcessor.class));
        this.bootProcessors.add(CabinServiceManager.get().getService(LibModuleMergeProcessor.class));
        this.bootProcessors.add(CabinServiceManager.get().getService(LibModuleLoadProcessor.class));
        this.bootProcessors.add(CabinServiceManager.get().getService(LibModuleExportProcessor.class));
        this.bootProcessors.add(CabinServiceManager.get().getService(ContainerStateExportProcessor.class));
        this.bootProcessors.add(CabinServiceManager.get().getService(BizModuleSetupProcessor.class));
    }

    @Override
    public void initialize(final CabinBootContext context) {
        for (Processor processor : bootProcessors) {
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Start to process " + processor.getClass().getName());
                }
                final long startTime = System.currentTimeMillis();
                processor.process(context);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Finished to process " + processor.getClass().getName() +
                            ", cost: " + (System.currentTimeMillis() - startTime) + "ms");
                }
            } catch (Throwable e) {
                throw new CabinRuntimeException(e);
            }
        }
    }
}

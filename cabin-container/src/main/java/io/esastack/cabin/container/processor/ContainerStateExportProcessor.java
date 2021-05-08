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

import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinContainerUtil;
import io.esastack.cabin.container.initialize.CabinBootContext;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public class ContainerStateExportProcessor implements Processor {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(BizModuleSetupProcessor.class);

    private volatile ClassLoaderService classLoaderService;

    @Override
    public void process(final CabinBootContext cabinBootContext) throws CabinRuntimeException {
        final ClassLoader bizClassloader = classLoaderService.getBizModuleClassLoader();
        if (bizClassloader == null) {
            throw new CabinRuntimeException("Biz Module has not been loaded!");
        }
        try {
            final Class<?> clazzOfBiz = bizClassloader.loadClass(CabinContainerUtil.class.getName());
            final Method initMethod = clazzOfBiz.getDeclaredMethod("init", Object.class, ClassLoader.class);
            initMethod.setAccessible(true);
            initMethod.invoke(null, cabinBootContext.getCabinContainer(), bizClassloader);
            initMethod.setAccessible(false);
        } catch (Throwable throwable) {
            final String msg = String.format(
                    "Failed to init {%s} of BizModuleClassloader", CabinContainerUtil.class.getName());
            LOGGER.error(msg, throwable);
            throw new CabinRuntimeException(msg, throwable);
        }
    }

    public void setClassLoaderService(final ClassLoaderService service) {
        classLoaderService = service;
    }
}

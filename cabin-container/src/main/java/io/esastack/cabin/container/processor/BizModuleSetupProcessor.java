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

import io.esastack.cabin.api.service.deploy.BizModuleLoadService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.util.RelaunchMarkUtil;
import io.esastack.cabin.container.domain.BizModule;
import io.esastack.cabin.container.initialize.CabinBootContext;

public class BizModuleSetupProcessor implements Processor {

    private volatile BizModuleLoadService<BizModule> bizModuleLoadService;

    public void process(final CabinBootContext cabinBootContext) throws CabinRuntimeException {
        final BizModule bizModule = bizModuleLoadService.getModule();
        try {
            //mark the RelaunchMarkUtil's state of the biz classloader, before the main() re-run
            RelaunchMarkUtil.markAsLaunched(bizModule.getClassLoader());
            bizModule.start();
        } catch (Throwable t) {
            throw new CabinRuntimeException("Failed to set up biz, caused by: ", t);
        }
    }

    public void setBizModuleLoadService(final BizModuleLoadService<BizModule> service) {
        bizModuleLoadService = service;
    }
}

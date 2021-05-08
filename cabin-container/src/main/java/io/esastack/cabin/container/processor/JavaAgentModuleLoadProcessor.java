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

import io.esastack.cabin.api.service.deploy.JavaAgentModuleLoadService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.container.initialize.CabinBootContext;

import java.net.URL;

public class JavaAgentModuleLoadProcessor implements Processor {

    private volatile JavaAgentModuleLoadService<?> javaAgentModuleLoadService;

    public void process(final CabinBootContext cabinBootContext) throws CabinRuntimeException {
        final URL[] agentUrls = cabinBootContext.getJavaAgentUrls();
        if (agentUrls != null && agentUrls.length > 0) {
            for (URL agentUrl : agentUrls) {
                javaAgentModuleLoadService.loadModule(agentUrl);
            }
        }
    }

    public void setClassLoaderService(final JavaAgentModuleLoadService<?> service) {
        javaAgentModuleLoadService = service;
    }
}

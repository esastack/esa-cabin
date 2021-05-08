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

import io.esastack.cabin.api.service.deploy.JavaAgentModuleFactoryService;
import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.container.domain.JavaAgentModule;

import java.net.URL;

public class JavaAgentModuleFactoryServiceImpl implements JavaAgentModuleFactoryService<JavaAgentModule> {

    private volatile ClassLoaderService classLoaderService;

    public void setClassLoaderService(final ClassLoaderService service) {
        classLoaderService = service;
    }

    @Override
    public JavaAgentModule createModule(final URL agentUrl) throws CabinRuntimeException {
        return JavaAgentModule.newBuilder()
                .url(agentUrl)
                .name("Java Agent Module")
                .classLoader(classLoaderService.createJavaAgentModuleClassLoader(agentUrl))
                .build();
    }
}

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
import io.esastack.cabin.api.service.deploy.JavaAgentModuleLoadService;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.container.domain.JavaAgentModule;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JavaAgentModuleLoadServiceImpl implements JavaAgentModuleLoadService<JavaAgentModule> {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(JavaAgentModuleLoadServiceImpl.class);
    private final ConcurrentMap<String, JavaAgentModule> javaAgentModules = new ConcurrentHashMap<>();
    private volatile JavaAgentModuleFactoryService<JavaAgentModule> moduleFactory;

    public void setModuleFactory(final JavaAgentModuleFactoryServiceImpl service) {
        moduleFactory = service;
    }

    @Override
    public void loadModule(final URL agentUrl) {
        final String agentModuleName = agentUrl.toExternalForm();
        if (javaAgentModules.containsKey(agentModuleName)) {
            LOGGER.error("Duplicated agent URL found for: " + agentModuleName);
            return;
        }

        if (javaAgentModules.computeIfAbsent(agentModuleName, agent -> moduleFactory.createModule(agentUrl)) != null) {
            LOGGER.error("Duplicated agent URL found for: " + agentModuleName);
        }
    }

    @Override
    public List<JavaAgentModule> getModules() {
        return new ArrayList<>(javaAgentModules.values());
    }
}

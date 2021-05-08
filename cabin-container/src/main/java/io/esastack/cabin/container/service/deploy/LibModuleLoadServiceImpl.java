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

import io.esastack.cabin.api.domain.Module;
import io.esastack.cabin.api.service.deploy.LibModuleFactoryService;
import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.container.domain.LibModule;
import io.esastack.cabin.loader.archive.Archive;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LibModuleLoadServiceImpl implements LibModuleLoadService {

    private static Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleLoadServiceImpl.class);

    private final ConcurrentMap<String, LibModule> modules = new ConcurrentHashMap<>();

    private volatile LibModuleFactoryService<LibModule> moduleFactory;

    public void setModuleFactory(final LibModuleFactoryServiceImpl service) {
        moduleFactory = service;
    }

    @Override
    public Module loadModule(final String name, final Archive archive) throws CabinRuntimeException {
        final LibModule module = moduleFactory.createModule(name, archive);
        final LibModule prevModule = modules.putIfAbsent(name, module);
        if (prevModule != null) {
            //TODO, clean/close the module which is new created
            throw new CabinRuntimeException("Duplicated module found for module name: " + name);
        }
        return module;
    }

    @Override
    public Module getModule(final String name) {
        return modules.get(name);
    }

    @Override
    public List<Module> getAllModules() {
        final List<Module> moduleList = new ArrayList<>(modules.values());
        Collections.sort(moduleList);
        return moduleList;
    }
}

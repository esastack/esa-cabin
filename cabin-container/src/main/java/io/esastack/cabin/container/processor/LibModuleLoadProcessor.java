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

import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.container.initialize.CabinBootContext;
import io.esastack.cabin.loader.archive.Archive;

import java.util.Map;

public class LibModuleLoadProcessor implements Processor {

    private volatile LibModuleLoadService libModuleLoadService;

    public void process(final CabinBootContext cabinBootContext) throws CabinRuntimeException {
        for (Map.Entry<String, Archive> entry : cabinBootContext.getModuleArchives().entrySet()) {
            libModuleLoadService.loadModule(entry.getKey(), entry.getValue());
        }
    }

    public void setLibModuleLoadService(final LibModuleLoadService service) {
        libModuleLoadService = service;
    }
}

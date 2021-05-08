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

import io.esastack.cabin.loader.archive.Archive;

import java.net.URL;
import java.util.Map;

public class CabinBootContext {

    private final URL[] moduleUrls;

    private final URL[] bizUrls;

    private final URL[] javaAgentUrls;

    private final String[] arguments;

    private final Object cabinContainer;

    private final Archive containerArchive;

    private volatile Map<String, Archive> moduleArchives;

    private CabinBootContext(final Builder builder) {
        this.moduleUrls = builder.moduleUrls;
        this.bizUrls = builder.bizUrls;
        this.javaAgentUrls = builder.javaAgentUrls;
        this.arguments = builder.arguments;
        this.cabinContainer = builder.cabinContainer;
        this.containerArchive = builder.containerArchive;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Object getCabinContainer() {
        return cabinContainer;
    }

    public Archive getContainerArchive() {
        return containerArchive;
    }

    public URL[] getModuleUrls() {
        return moduleUrls;
    }

    public URL[] getBizUrls() {
        return bizUrls;
    }

    public URL[] getJavaAgentUrls() {
        return javaAgentUrls;
    }

    public String[] getArguments() {
        return arguments;
    }

    public Map<String, Archive> getModuleArchives() {
        return moduleArchives;
    }

    public void setModuleArchives(final Map<String, Archive> moduleArchives) {
        this.moduleArchives = moduleArchives;
    }

    public static class Builder {

        private URL[] moduleUrls;

        private URL[] bizUrls;

        private URL[] javaAgentUrls;

        private String[] arguments;

        private Object cabinContainer;

        private Archive containerArchive;

        public Builder moduleUrls(final URL[] moduleUrls) {
            this.moduleUrls = moduleUrls;
            return this;
        }

        public Builder bizUrls(final URL[] bizUrls) {
            this.bizUrls = bizUrls;
            return this;
        }

        public Builder javaAgentUrls(final URL[] javaAgentUrls) {
            this.javaAgentUrls = javaAgentUrls;
            return this;
        }

        public Builder arguments(final String[] arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder cabinContainer(final Object cabinContainer) {
            this.cabinContainer = cabinContainer;
            return this;
        }

        public Builder containerArchive(final Archive containerArchive) {
            this.containerArchive = containerArchive;
            return this;
        }

        public CabinBootContext build() {
            return new CabinBootContext(this);
        }
    }
}

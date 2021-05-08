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
package io.esastack.cabin.container.domain;

import io.esastack.cabin.api.domain.Module;

import java.net.URL;

public class JavaAgentModule implements Module {

    private final URL url;

    private final String name;

    private final ClassLoader classLoader;

    private JavaAgentModule(final Builder builder) {
        this.url = builder.url;
        this.name = builder.name;
        this.classLoader = builder.classLoader;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public URL getUrl() {
        return url;
    }

    public static class Builder {

        private URL url;

        private String name;

        private ClassLoader classLoader;

        public Builder url(final URL url) {
            this.url = url;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public JavaAgentModule build() {
            return new JavaAgentModule(this);
        }
    }
}

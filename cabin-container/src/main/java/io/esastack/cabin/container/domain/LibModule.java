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
import io.esastack.cabin.loader.archive.Archive;

import java.util.List;

public class LibModule implements Module {

    private final String name;

    private final int priority;

    private final Archive archive;

    private final ExportInfo exportInfo;

    private final ImportInfo importInfo;

    private final List<String> providedClasses;

    private final ClassLoader classLoader;

    private LibModule(final Builder builder) {
        this.name = builder.name;
        this.archive = builder.archive;
        this.priority = builder.priority;
        this.exportInfo = builder.exportInfo;
        this.importInfo = builder.importInfo;
        this.classLoader = builder.classLoader;
        this.providedClasses = builder.providedClasses;
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

    public int getPriority() {
        return this.priority;
    }

    public Archive getArchive() {
        return archive;
    }

    public ExportInfo getExportInfo() {
        return this.exportInfo;
    }

    public ImportInfo getImportInfo() {
        return this.importInfo;
    }

    public List<String> getProvidedClasses() {
        return providedClasses;
    }

    @Override
    public int compareTo(final Module o) {
        return this.priority - ((LibModule) o).priority;

    }

    public static class Builder {

        private String name;

        private int priority;

        private Archive archive;

        private ExportInfo exportInfo;

        private ImportInfo importInfo;

        private List<String> providedClasses;

        private ClassLoader classLoader;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder priority(final int priority) {
            this.priority = priority;
            return this;
        }

        public Builder archive(final Archive archive) {
            this.archive = archive;
            return this;
        }

        public Builder exportInfo(final ExportInfo exportInfo) {
            this.exportInfo = exportInfo;
            return this;
        }

        public Builder importInfo(final ImportInfo importInfo) {
            this.importInfo = importInfo;
            return this;
        }

        public Builder providedClasses(final List<String> providedClasses) {
            this.providedClasses = providedClasses;
            return this;
        }

        public Builder classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public LibModule build() {
            return new LibModule(this);
        }
    }
}

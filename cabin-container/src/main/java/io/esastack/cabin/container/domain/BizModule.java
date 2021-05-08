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
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.forkjoinpool.ForkJoinPoolInitializer;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.container.common.MainMethodRunner;
import org.slf4j.Logger;

import java.net.URL;

public class BizModule implements Module {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(BizModule.class);

    private final URL[] urls;

    private final String name;

    private final String mainClass;

    private final String mainMethod;

    private final String[] arguments;

    private final ClassLoader classLoader;

    private final boolean unitTest;

    private BizModule(final Builder builder) {
        this.urls = builder.urls;
        this.name = builder.name;
        this.mainClass = builder.mainClass;
        this.mainMethod = builder.mainMethod;
        this.arguments = builder.arguments;
        this.classLoader = builder.classLoader;
        this.unitTest = builder.unitTest;
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

    public URL[] getUrls() {
        return urls;
    }

    public String[] getArguments() {
        return arguments;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getMainMethod() {
        return mainMethod;
    }

    public boolean isUnitTest() {
        return unitTest;
    }

    public void start() throws Throwable {
        final ClassLoader oldTCCL = ClassLoaderUtils.pushTCCL(classLoader);
        try {
            if (CabinStringUtil.isBlank(mainClass)) {
                throw new CabinRuntimeException("Main-class not found, Could not start biz module");
            }

            ForkJoinPoolInitializer.init();

            if (isUnitTest()) {
                LOGGER.info("Main-method is null or empty, this is running in unit test!");
                return;
            }

            final MainMethodRunner mainMethodRunner = new MainMethodRunner(mainClass, mainMethod, arguments);
            mainMethodRunner.run();
        } finally {
            ClassLoaderUtils.setTCCL(oldTCCL);
        }
    }

    public static class Builder {
        private URL[] urls;

        private String name;

        private String mainClass;

        private String mainMethod;

        private String[] arguments;

        private ClassLoader classLoader;

        private boolean unitTest;

        public Builder urls(final URL[] urls) {
            this.urls = urls;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder mainClass(final String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public Builder mainMethod(final String mainMethod) {
            this.mainMethod = mainMethod;
            return this;
        }

        public Builder arguments(final String[] arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder unitTest(final boolean isUnitTest) {
            this.unitTest = isUnitTest;
            return this;
        }

        public BizModule build() {
            return new BizModule(this);
        }
    }
}

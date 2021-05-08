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
package io.esastack.cabin.api.service.loader;

import java.net.URL;

public class BizModuleClassLoaderParam {

    private final URL[] urls;

    private final boolean isUnitTest;

    private BizModuleClassLoaderParam(Builder builder) {
        this.urls = builder.urls;
        this.isUnitTest = builder.isUnitTest;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public URL[] getUrls() {
        return urls;
    }

    public boolean isUnitTest() {
        return isUnitTest;
    }

    public static class Builder {

        private URL[] urls;

        private boolean isUnitTest;

        public Builder urls(URL[] urls) {
            this.urls = urls;
            return this;
        }

        public Builder isUnitTest(boolean isUnitTest) {
            this.isUnitTest = isUnitTest;
            return this;
        }

        public BizModuleClassLoaderParam build() {
            return new BizModuleClassLoaderParam(this);
        }
    }
}


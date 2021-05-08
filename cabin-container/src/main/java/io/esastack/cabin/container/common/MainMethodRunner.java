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
package io.esastack.cabin.container.common;

import java.lang.reflect.Method;

public class MainMethodRunner {

    private final String mainClassName;

    private final String mainMethodName;

    private final String[] args;

    public MainMethodRunner(final String mainClass, final String mainMethod, final String[] args) {
        this.mainClassName = mainClass;
        this.mainMethodName = mainMethod;
        this.args = (args == null ? null : args.clone());
    }

    public void run() throws Exception {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Class<?> mainClass = classLoader.loadClass(this.mainClassName);
        final Method mainMethod = mainClass.getDeclaredMethod(mainMethodName, String[].class);
        mainMethod.invoke(null, new Object[]{this.args});
    }
}

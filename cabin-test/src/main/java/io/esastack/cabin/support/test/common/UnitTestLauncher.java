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
package io.esastack.cabin.support.test.common;

import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.support.bootstrap.CabinClasspathLauncher;

import java.lang.reflect.Method;
import java.net.URL;

import static io.esastack.cabin.common.constant.Constants.CABIN_UNIT_TEST_MAIN_CLASSNAME;
import static io.esastack.cabin.common.constant.Constants.CABIN_UNIT_TEST_MAIN_METHOD;

public class UnitTestLauncher extends CabinClasspathLauncher {

    private static volatile Object cabinContainer;

    private static volatile ClassLoader testBizClassLoader;

    private UnitTestLauncher(final URL[] urls) {
        super(CABIN_UNIT_TEST_MAIN_CLASSNAME, urls);
    }

    public static synchronized void start() {
        if (cabinContainer == null) {
            try {
                final UnitTestLauncher launcher = new UnitTestLauncher(ClassLoaderUtils.getApplicationClassPaths());
                cabinContainer = launcher.launch(new String[0]);
            } catch (Exception e) {
                throw new CabinRuntimeException("Failed to start UnitTestLauncher!", e);
            }
        }
    }

    public static synchronized void stop() {
        try {
            final Method method = cabinContainer.getClass().getDeclaredMethod("stop");
            method.invoke(cabinContainer);
        } catch (Throwable ex) {
            throw new CabinRuntimeException("Failed to stop CabinContainer!", ex.getCause());
        }
    }

    public static synchronized boolean isStarted() {
        return cabinContainer != null;
    }

    public static ClassLoader getTestBizClassLoader() {
        if (testBizClassLoader == null) {
            synchronized (UnitTestLauncher.class) {
                if (testBizClassLoader == null) {
                    try {
                        final Method method = cabinContainer.getClass()
                                .getDeclaredMethod("getBizModuleClassLoader");
                        testBizClassLoader = (ClassLoader) method.invoke(cabinContainer);
                    } catch (Throwable ex) {
                        throw new RuntimeException("Failed to get TestBizClassLoader of CabinContainer", ex.getCause());
                    }
                }
            }
        }
        return testBizClassLoader;
    }

    /**
     * main method is null means there is no need to start biz, just start the Cabin Container
     */
    @Override
    protected String findAppMainMethod() {
        return CABIN_UNIT_TEST_MAIN_METHOD;
    }

}

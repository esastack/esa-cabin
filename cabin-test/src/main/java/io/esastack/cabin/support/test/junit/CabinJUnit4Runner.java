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
package io.esastack.cabin.support.test.junit;

import io.esastack.cabin.support.test.common.UnitTestLauncher;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

/**
 * Used to run unit test
 */
public class CabinJUnit4Runner extends BlockJUnit4ClassRunner {

    public CabinJUnit4Runner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected TestClass createTestClass(Class<?> testClass) {
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        try {
            UnitTestLauncher.start();
            final ClassLoader bizModuleClassLoader = UnitTestLauncher.getTestBizClassLoader();
            Thread.currentThread().setContextClassLoader(bizModuleClassLoader);
            return super.createTestClass(bizModuleClassLoader.loadClass(testClass.getName()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
    }

    @Override
    public void run(RunNotifier notifier) {
        notifier.addListener(JUnitExecutionListener.getRunListener());
        super.run(notifier);
    }
}

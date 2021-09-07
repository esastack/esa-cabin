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
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;

/**
 * Why do we load the real Runner with TestBizClassLoader?
 * 1. The test class must be loaded by TestBizClassLoader;
 * 2. Spring/springboot load the beans with TCCL class loader, in order to assign the bean to
 * ref field(which is loaded by TestBizClassLoader) of test class, TCCL must be TestBizClassLoader;
 * 3. Springboot scan and load the SPI implementations with TCCL, if the Spring classes are loaded by AppClassloader,
 * the SPI implementations could not be assigned to the SPI refs.
 * So, the best way is to load both Spring classes and test class with TestBizClassLoader.
 */
public class CabinSpringRunner extends Runner implements Filterable, Sortable {

    private static final String SPRING_RUNNER = "org.springframework.test.context.junit4.SpringJUnit4ClassRunner";

    private final Runner delegate;

    public CabinSpringRunner(final Class<?> testClass) {
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        try {
            UnitTestLauncher.start();
            Thread.currentThread().setContextClassLoader(UnitTestLauncher.getTestBizClassLoader());
            final Class<?> springRunnerClass = UnitTestLauncher.getTestBizClassLoader().loadClass(SPRING_RUNNER);
            final Class<?> testClazz = UnitTestLauncher.getTestBizClassLoader().loadClass(testClass.getName());
            this.delegate = (Runner) springRunnerClass.getConstructor(Class.class).newInstance(testClazz);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    /**
     * In the run method, TCCL would be used to load Spring SPI classes,
     * so it should be set to UnitTestModuleClassLoader
     */
    @Override
    public void run(RunNotifier notifier) {
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(UnitTestLauncher.getTestBizClassLoader());
            notifier.addListener(JUnitExecutionListener.getRunListener());
            delegate.run(notifier);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        ((Filterable) delegate).filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        ((Sortable) delegate).sort(sorter);
    }
}

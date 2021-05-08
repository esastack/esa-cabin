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
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

/**
 * Set the TCCL to UnitTestModuleClassLoader before a single TEST is executed, and recover it to AppClassLoader after
 * TEST finished.
 */
public class JUnitExecutionListener extends RunListener {

    private static final String CABIN_JUNIT4_RUNNER = CabinJUnit4Runner.class.getName();
    private static final String CABIN_SPRING_RUNNER = CabinSpringRunner.class.getName();

    private static volatile RunListener instance;

    private JUnitExecutionListener() {
    }

    public static RunListener getRunListener() {
        if (instance == null) {
            synchronized (JUnitExecutionListener.class) {
                if (instance == null) {
                    instance = new JUnitExecutionListener();
                }
            }
        }
        return instance;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        if (isCabinRunnerUsed(description)) {
            Thread.currentThread().setContextClassLoader(UnitTestLauncher.getTestBizClassLoader());
        } else {
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
        super.testStarted(description);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
    }

    protected boolean isCabinRunnerUsed(Description description) {
        final RunWith runWith = description.getTestClass().getAnnotation(RunWith.class);
        if (runWith == null) {
            return false;
        }
        final String runnerName = runWith.value().getName();
        return CABIN_JUNIT4_RUNNER.equals(runnerName) || CABIN_SPRING_RUNNER.equals(runnerName);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        super.testRunFinished(result);
        UnitTestLauncher.stop();
    }
}

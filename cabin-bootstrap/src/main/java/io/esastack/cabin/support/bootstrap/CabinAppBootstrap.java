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
package io.esastack.cabin.support.bootstrap;

import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.common.util.RelaunchMarkUtil;
import io.esastack.cabin.support.bootstrap.thread.IsolatedThreadGroup;
import io.esastack.cabin.support.bootstrap.thread.ReLaunchRunner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CabinAppBootstrap {

    private static final String REBOOT_METHOD_NAME = "reboot";

    private static final String REBOOT_THREAD_NAME = "main";

    private static volatile String mainClass;

    public static void run(final String[] args) {
        if (args == null) {
            throw new CabinRuntimeException("args should not be null");
        }
        if (!RelaunchMarkUtil.isRelaunched()) {
            mainClass = deduceMainClass();
            final String bootClazz = CabinAppBootstrap.class.getName();
            final IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(bootClazz);
            final ReLaunchRunner runner = new ReLaunchRunner(bootClazz, REBOOT_METHOD_NAME, args);
            final Thread rebootThread = new Thread(threadGroup, runner, REBOOT_THREAD_NAME);
            rebootThread.start();
            ReLaunchRunner.join(threadGroup);
            threadGroup.rethrowUncaughtException();
            System.exit(0);
        }
    }

    private static void reboot(final String[] args) throws Exception {
        new CabinClasspathLauncher(mainClass, ClassLoaderUtils.getSystemClassPaths()).launch(args);
    }

    private static String deduceMainClass() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if ("main".equals(element.getMethodName())) {
                try {
                    final String mainClazz = element.getClassName();
                    final Class<?> mainClass = Class.forName(mainClazz);
                    final Method method = mainClass.getDeclaredMethod("main", String[].class);
                    if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                        return mainClazz;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        throw new IllegalStateException("Unable to find main class");
    }
}

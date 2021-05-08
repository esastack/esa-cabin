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
package io.esastack.cabin.support.bootstrap.thread;

import java.lang.reflect.Method;

public class ReLaunchRunner implements Runnable {

    private final String bootClazz;

    private final String bootMethod;

    private final String[] args;

    public ReLaunchRunner(final String bootClazz, final String bootMethod, final String[] args) {
        this.bootClazz = bootClazz;
        this.bootMethod = bootMethod;
        this.args = args;
    }

    public static void join(final ThreadGroup threadGroup) {
        boolean hasNonDaemonThreads;
        do {
            hasNonDaemonThreads = false;
            Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            for (Thread thread : threads) {
                if (thread != null && !thread.isDaemon()) {
                    try {
                        hasNonDaemonThreads = true;
                        thread.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (hasNonDaemonThreads);
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        final ClassLoader classLoader = thread.getContextClassLoader();
        try {
            final Class<?> startClass = classLoader.loadClass(bootClazz);
            final Method entryMethod = startClass.getDeclaredMethod(bootMethod, String[].class);
            entryMethod.setAccessible(true);
            entryMethod.invoke(null, (Object) this.args);
        } catch (NoSuchMethodException ex) {
            final Exception wrappedEx = new Exception(
                    "The specified mainClass doesn't contain a " + "main method with appropriate signature.", ex);
            thread.getThreadGroup().uncaughtException(thread, wrappedEx);
        } catch (Throwable ex) {
            thread.getThreadGroup().uncaughtException(thread, ex);
        }
    }
}

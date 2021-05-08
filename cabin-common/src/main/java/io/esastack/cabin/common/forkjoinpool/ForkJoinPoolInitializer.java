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
package io.esastack.cabin.common.forkjoinpool;

import io.esastack.cabin.common.log.CabinLoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ForkJoinPool;

public class ForkJoinPoolInitializer {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(ForkJoinPoolInitializer.class);

    private static final String FORK_JOIN_POOL_FACTORY = CabinForkJoinWorkerThreadFactory.class.getName();

    public static void init() {
        final String javaVersion = System.getProperty("java.version");
        LOGGER.info("Current JDK version is " + javaVersion);
        try {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory", FORK_JOIN_POOL_FACTORY);
            ForkJoinPool.commonPool().execute(() -> {
                LOGGER.info("ForkJoinPool factory is set to {}, thread context classloader is {}.",
                        FORK_JOIN_POOL_FACTORY, Thread.currentThread().getContextClassLoader());
            });
        } catch (Throwable t) {
            LOGGER.error("Failed to init ForkJoinPool common pool!", t);
        } finally {
            System.clearProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
        }
    }
}

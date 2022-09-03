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
package io.esastack.cabin.common.util;

import java.lang.reflect.Field;

/**
 * This class is used to check whether the program are running in the cabin container environment.
 * While the application start in the IDE:
 * The {@link #isRelaunched()} would return false at the first time of the application main class running,
 * so the re-run operation continues; after cabin create BizModuleClassLoader and before starting the application
 * main class again, the {@link #relaunched} field is set to true, so thr re-run operation would be ignored.
 * While using fatjar to setup a application:
 * The application main class would run after biz module exported, the {@link #relaunched} field is set to true,
 * no re-run operation would happen.
 */
public class RelaunchMarkUtil {

    private static volatile boolean relaunched = false;

    public static void markAsLaunched(ClassLoader bizClassLoader) {
        try {
            final Class<?> clazz = bizClassLoader.loadClass("io.esastack.cabin.common.util.RelaunchMarkUtil");
            final Field field = clazz.getDeclaredField("relaunched");
            field.setAccessible(true);
            field.set(null, true);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static boolean isRelaunched() {
        return relaunched;
    }
}

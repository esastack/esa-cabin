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

public class RelaunchMarkUtil {

    private static volatile boolean relaunched = false;

    public static void markAsLaunched(ClassLoader classLoader) {
        try {
            final Class<?> clazz = classLoader.loadClass(RelaunchMarkUtil.class.getCanonicalName());
            final Field field = clazz.getDeclaredField("relaunched");
            field.setAccessible(true);
            field.set(null, true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean isRelaunched() {
        return relaunched;
    }
}

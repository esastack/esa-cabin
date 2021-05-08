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

import java.net.URL;

public class CabinStringUtil {

    public static String urlsToString(final String prefix, final URL... urls) {
        final StringBuilder sb = new StringBuilder(prefix);
        if (urls != null && urls.length > 0) {
            for (URL url : urls) {
                sb.append("\n");
                sb.append(url.toExternalForm());
            }
        }
        return sb.toString();
    }

    public static String mergeStringArray(final String prefix, final String... array) {
        final StringBuilder sb = new StringBuilder(prefix);
        if (array != null && array.length > 0) {
            for (String elem : array) {
                sb.append("\n");
                sb.append(elem);
            }
        }
        return sb.toString();
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen = length(cs);
        if (strLen == 0) {
            return true;
        } else {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}

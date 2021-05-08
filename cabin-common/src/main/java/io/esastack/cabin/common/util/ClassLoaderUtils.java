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

import io.esastack.cabin.common.constant.Constants;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassLoaderUtils {

    public static ClassLoader pushTCCL(final ClassLoader classLoader) {
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        return oldTCCL;
    }

    public static void setTCCL(final ClassLoader classLoader) {
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    public static URL[] getSystemClassPaths() throws MalformedURLException {
        final String[] classPaths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
        final URL[] urls = new URL[classPaths.length];
        for (int i = 0; i < classPaths.length; i++) {
            urls[i] = new File(classPaths[i]).toURI().toURL();
        }
        return urls;
    }

    /**
     * the java agent premain setup arguments format: -javaagent:{jarpath}[=argsString]
     */
    public static URL[] getAgentClassPath() {
        final List<String> inputArguments = AccessController.doPrivileged(
                (PrivilegedAction<List<String>>) () -> ManagementFactory.getRuntimeMXBean().getInputArguments());
        final Set<String> agentPaths = new HashSet<>();
        for (String argument : inputArguments) {
            if (!argument.startsWith(Constants.JAVA_AGENT_MARK)) {
                continue;
            }
            agentPaths.add(
                    argument.substring(Constants.JAVA_AGENT_MARK.length()).split(Constants.JAVA_AGENT_OPTION_MARK)[0]);
        }
        return agentPaths.stream().map(path -> {
            try {
                return new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);
    }
}

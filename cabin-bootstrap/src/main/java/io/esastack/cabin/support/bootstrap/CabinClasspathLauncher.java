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

import io.esastack.cabin.common.constant.Constants;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.archive.JarFileArchive;
import io.esastack.cabin.loader.util.ArchiveUtils;
import io.esastack.cabin.support.boot.launcher.AbstractLauncher;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class CabinClasspathLauncher extends AbstractLauncher {

    private final String mainClass;

    private final URL[] classpathUrls;

    private final String containerUrl;

    private final String[] moduleUrls;

    public CabinClasspathLauncher(final String mainClass, final URL[] urls) {
        this.mainClass = mainClass;
        this.classpathUrls = urls;
        this.containerUrl = findContainerUrl();
        this.moduleUrls = findModuleURLs();
    }

    @Override
    protected Object createCabinContainer(final Class<?> cabinContainerClass) throws Exception {
        return cabinContainerClass
                .getConstructor(String.class, String[].class, String[].class, String[].class, String[].class)
                .newInstance(containerUrl, moduleUrls, findBizUrls(), findJavaAgentUrls(), arguments);
    }

    @Override
    protected String findAppMainClass() {
        return mainClass;
    }

    @Override
    protected Archive findContainerArchive() throws Exception {
        return new JarFileArchive(new File(new URL(containerUrl).getFile()));
    }

    @Override
    protected boolean lazyLoadExportClass() {
        return true;
    }

    private String findContainerUrl() {
        final List<String> containerUrls = new ArrayList<>();
        try {
            for (URL url : classpathUrls) {
                final String path = getDecodedPath(url.getFile());
                if (path.endsWith(Constants.JAR_FILE_SUFFIX)) {
                    try (JarFile jarFile = new JarFile(path)) {
                        if (ArchiveUtils.isCabinContainerJar(jarFile)) {
                            containerUrls.add(getDecodedPath(url.toExternalForm()));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new CabinRuntimeException("Failed to find Cabin container Jar File", t);
        }

        if (containerUrls.size() == 0) {
            throw new CabinRuntimeException("No Cabin container Jar File found in classpath urls");
        }

        if (containerUrls.size() > 1) {
            throw new CabinRuntimeException("Multiple Cabin container Jar Files found in classpath urls");
        }

        return containerUrls.get(0);
    }

    private String[] findModuleURLs() {
        final List<String> filteredUrls = new ArrayList<>();
        try {
            for (URL url : classpathUrls) {
                final String path = getDecodedPath(url.getFile());
                if (path.endsWith(Constants.JAR_FILE_SUFFIX)) {
                    try (JarFile jarFile = new JarFile(path)) {
                        if (ArchiveUtils.isCabinModuleJar(jarFile)) {
                            filteredUrls.add(getDecodedPath(url.toExternalForm()));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new CabinRuntimeException("Failed tp find Cabin container Jar File", t);
        }
        return filteredUrls.toArray(new String[0]);
    }

    /**
     * java agent urls should be filtered
     */
    private String[] findBizUrls() throws Exception {
        final List<String> filteredUrls = new ArrayList<>();
        for (URL url : classpathUrls) {
            final String urlString = getDecodedPath(url.toExternalForm());
            if (containerUrl.equals(urlString)) {
                continue;
            }
            boolean matched = false;
            for (String moduleUrl : moduleUrls) {
                if (moduleUrl.equals(urlString)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                for (URL agentUrl : agentUrls) {
                    if (getDecodedPath(agentUrl.getFile()).equals(urlString)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                filteredUrls.add(urlString);
            }
        }
        return filteredUrls.toArray(new String[0]);
    }

    private String getDecodedPath(final String path) throws UnsupportedEncodingException {
        return URLDecoder.decode(path, "UTF-8");
    }
}

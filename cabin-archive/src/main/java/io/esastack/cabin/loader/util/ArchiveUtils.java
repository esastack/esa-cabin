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
package io.esastack.cabin.loader.util;

import io.esastack.cabin.common.constant.Constants;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.archive.ExplodedArchive;
import io.esastack.cabin.loader.archive.JarFileArchive;
import io.esastack.cabin.loader.jar.Handler;
import io.esastack.cabin.loader.jar.JarFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.jar.Attributes;

import static io.esastack.cabin.common.constant.Constants.*;

public class ArchiveUtils {

    private static final String URL_PROTOCOL_FILE = "file";

    private static final String URL_PROTOCOL_VFSFILE = "vfsfile";

    private static final String URL_PROTOCOL_VFS = "vfs";

    /**
     * @param url if it's a normal url. If it's a dir, an ExplodedArchive is created, else a JarFileArchive is created.
     *            if it's a fat jar url, such as "jar:file:/tmp/xxx.jar/dir1/dir2!/yyy.jar!/" or
     *            "jar:file:/tmp/xxx.jar/dir1/dir2!/yyy.jar!/xxx.class", the Archive represents nest jar "yyy.jar"
     *            is returned.
     */
    public static Archive createArchiveFromUrl(URL url) throws IOException {
        try {
            if (isFileURL(url)) {
                File file = getFile(url);
                if (file.isDirectory()) {
                    return new ExplodedArchive(file);
                } else {
                    return new JarFileArchive(file);
                }
            }
            JarFile rootJarFile = Handler.getRootJarFileFromUrl(url);
            Archive parentArchive = new JarFileArchive(rootJarFile);
            while (true) {
                int sepratorIndex;
                String substring = url.toString().substring(parentArchive.getUrl().toString().length());
                if (substring.isEmpty() || (sepratorIndex = substring.indexOf(Constants.FILE_IN_JAR_SPLITTER)) == -1) {
                    return parentArchive;
                }
                String entryName = substring.substring(0, sepratorIndex);
                List<Archive> nestedArchives = parentArchive.getNestedArchives(entry -> {
                    if (entry.getName().equals(entryName)) {
                        return true;
                    }
                    return entry.isDirectory() && entry.getName().equals(entryName + '/');
                });
                if (!nestedArchives.isEmpty()) {
                    parentArchive = nestedArchives.get(0);
                }
            }

        } catch (Throwable e) {
            throw new IOException("createArchiveFromUrl error, url: " + url, e);
        }
    }

    public static File getFile(URL resourceUrl) throws FileNotFoundException {
        return getFile(resourceUrl, "URL");
    }

    public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource URL must not be null");
        }
        if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
            throw new FileNotFoundException(
                    description + " cannot be resolved to absolute file path " +
                            "because it does not reside in the file system: " + resourceUrl);
        }
        try {
            return new File(resourceUrl.toURI().getSchemeSpecificPart());
        } catch (URISyntaxException ex) {
            // Fallback for URLs that are not valid URIs (should hardly ever happen).
            return new File(resourceUrl.getFile());
        }
    }

    public static boolean isFileURL(URL url) {
        String protocol = url.getProtocol();
        return (URL_PROTOCOL_FILE.equals(protocol) || URL_PROTOCOL_VFSFILE.equals(protocol) ||
                URL_PROTOCOL_VFS.equals(protocol));
    }

    public static Archive extractContainerArchive(final Archive executableArchive) {
        final List<Archive> containerRawArchives;
        try {
            containerRawArchives = executableArchive.getNestedArchives(entry -> !entry.isDirectory() &&
                    entry.getName().startsWith(CABIN_CORE_DIRECTORY));
        } catch (IOException e) {
            throw new CabinRuntimeException("Failed to get container archive from executable jar", e);
        }

        if (containerRawArchives == null || containerRawArchives.size() == 0) {
            throw new CabinRuntimeException("Container archive not Found");
        }

        if (containerRawArchives.size() != 1) {
            throw new CabinRuntimeException("Container archive count is more than one");
        }

        return containerRawArchives.get(0);
    }

    public static URL[] extractModuleURLs(final Archive executableArchive) {
        final List<Archive> moduleRawArchives;
        try {
            moduleRawArchives = executableArchive.getNestedArchives(entry -> !entry.isDirectory() &&
                    entry.getName().startsWith(CABIN_MODULE_DIRECTORY));
        } catch (IOException e) {
            throw new CabinRuntimeException("Failed to get container archive from executable jar", e);
        }

        if (moduleRawArchives == null || moduleRawArchives.size() == 0) {
            return null;
        }

        final URL[] urls = new URL[moduleRawArchives.size()];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = moduleRawArchives.get(i).getUrl();
            } catch (MalformedURLException e) {
                throw new CabinRuntimeException("Failed to get URL of module archive!");
            }
        }
        return urls;
    }

    public static URL[] extractBizUrls(final Archive executableArchive) {
        try {
            final List<Archive> bizArchives = executableArchive.getNestedArchives(entry -> {
                if (entry.isDirectory()) {
                    return entry.getName().startsWith(APP_CLASSES_DIRECTORY);
                }
                return entry.getName().startsWith(APP_LIB_DIRECTORY);
            });
            final URL[] bizUrls = new URL[bizArchives.size()];
            for (int i = 0; i < bizArchives.size(); i++) {
                bizUrls[i] = bizArchives.get(i).getUrl();
            }
            return bizUrls;
        } catch (IOException e) {
            throw new CabinRuntimeException("Failed to get biz archive from executable jar", e);
        }
    }

    public static boolean isCabinContainerJar(final java.util.jar.JarFile jarFile) throws Exception {
        return jarFile.getManifest() != null &&
                jarFile.getManifest().getMainAttributes().getValue(MANIFEST_CABIN_VERSION) != null;
    }

    public static boolean isCabinModuleJar(final java.util.jar.JarFile jarFile) throws Exception {
        final Attributes attributes = jarFile.getManifest().getMainAttributes();
        return attributes.getValue(MANIFEST_MODULE_NAME) != null &&
                attributes.getValue(MANIFEST_MODULE_GROUP_ID) != null &&
                attributes.getValue(MANIFEST_MODULE_ARTIFACT_ID) != null &&
                (attributes.getValue("export-packages") != null ||
                        attributes.getValue("export-classes") != null);
    }
}

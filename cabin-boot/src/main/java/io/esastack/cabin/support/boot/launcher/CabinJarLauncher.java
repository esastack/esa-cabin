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
package io.esastack.cabin.support.boot.launcher;

import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.util.ArchiveUtils;

import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.Manifest;

import static io.esastack.cabin.common.constant.Constants.MANIFEST_START_CLASS;

public class CabinJarLauncher extends AbstractLauncher {

    /**
     * URL of this archive is opened by the default URLStreamHandler, so it could not be used to create CabinContainer.
     */
    private final Archive executableArchive;

    private CabinJarLauncher() {
        try {
            this.executableArchive = deduceArchive();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected CabinJarLauncher(final Archive archive) {
        try {
            this.executableArchive = archive;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void main(final String[] args) {
        try {
            final CabinJarLauncher cabinJarLauncher = new CabinJarLauncher();
            cabinJarLauncher.launch(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    protected Object createCabinContainer(final Class<?> cabinContainerClass) throws Exception {
        return cabinContainerClass.getConstructor(String.class, String[].class, String[].class)
                .newInstance(executableArchive.getUrl().toExternalForm(), findJavaAgentUrls(), arguments);
    }

    private Archive deduceArchive() throws Exception {
        final ProtectionDomain protectionDomain = getClass().getProtectionDomain();
        final CodeSource codeSource = protectionDomain.getCodeSource();
        final URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
        if (location == null) {
            throw new IllegalStateException("Unable to determine code source for " + getClass());
        }
        return ArchiveUtils.createArchiveFromUrl(location.toURL());
    }

    @Override
    protected String findAppMainClass() throws Exception {
        final Manifest manifest = this.executableArchive.getManifest();
        if (manifest == null) {
            throw new CabinRuntimeException("Invalid Executable Jar, no manifest provided");
        }
        final String mainClass = manifest.getMainAttributes().getValue(MANIFEST_START_CLASS);
        if (mainClass == null) {
            throw new CabinRuntimeException("Invalid Executable Jar, Manifest doesn't provide 'Start-Class' attribute");
        }

        return mainClass;
    }

    @Override
    protected Archive findContainerArchive() {
        return ArchiveUtils.extractContainerArchive(executableArchive);
    }

}

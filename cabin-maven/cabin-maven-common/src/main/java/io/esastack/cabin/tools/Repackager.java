/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
package io.esastack.cabin.tools;

import io.esastack.cabin.common.util.FileUtils;
import io.esastack.cabin.loader.util.ArchiveUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static io.esastack.cabin.common.constant.Constants.*;

public class Repackager {

    private static final String CABIN_VERSION_ATTRIBUTE = "Cabin-Version";

    private static final String CABIN_CONTAINER_ROOT = "Cabin-Container-Root";

    private static final byte[] ZIP_FILE_HEADER = new byte[]{'P', 'K', 3, 4};

    private final File source;
    private final Log logger;
    private final List<Library> moduleLibraries = new ArrayList<>();
    private final List<Library> bizLibraries = new ArrayList<>();
    private String startClass;
    private File executableFatJar;
    private File baseDir;
    private boolean packageProvided;
    private String cabinVersion;
    private Library containerLibrary;

    public Repackager(File source, Log logger) {
        if (source == null || logger == null) {
            throw new IllegalArgumentException("Source file must be provided");
        }
        if (!source.exists() || !source.isFile()) {
            throw new IllegalArgumentException("Source must refer to an existing file, " + "got"
                    + source.getAbsolutePath());
        }
        this.source = source.getAbsoluteFile();
        this.logger = logger;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isZip(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                return isZip(fileInputStream);
            } finally {
                fileInputStream.close();
            }
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean isZip(InputStream inputStream) throws IOException {
        for (int i = 0; i < ZIP_FILE_HEADER.length; i++) {
            if (inputStream.read() != ZIP_FILE_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    public void setCabinVersion(String cabinVersion) {
        this.cabinVersion = cabinVersion;
    }

    public void setStartClass(String startClass) {
        this.startClass = startClass;
    }

    public void repackage(File appDestination, Libraries libraries) throws IOException {

        if (appDestination == null || appDestination.isDirectory()) {
            throw new IllegalArgumentException("Invalid destination");
        }
        if (libraries == null) {
            throw new IllegalArgumentException("Libraries must not be null");
        }
        if (alreadyRepackaged()) {
            return;
        }

        executableFatJar = appDestination;

        libraries.doWithLibraries((library) -> {

            if (LibraryScope.PROVIDED.equals(library.getScope()) && !isPackageProvided()) {
                logger.debug("Ignore provided library: " + library.getFile());
                return;
            }

            if (!isZip(library.getFile())) {
                logger.debug("Ignore none zip library: " + library.getFile());
                return;
            }

            try (JarFile jarFile = new JarFile(library.getFile())) {
                if (ArchiveUtils.isCabinContainerJar(jarFile)) {
                    logger.debug("Found cabin core library: " + library.getFile());
                    if (containerLibrary != null) {
                        throw new RuntimeException(String.format("Duplicate cabin-core dependency found, %s and %s",
                                containerLibrary.getFile().getAbsolutePath(), library.getFile().getAbsolutePath()));
                    }
                    library.setScope(LibraryScope.CONTAINER);
                    containerLibrary = library;
                } else if (ArchiveUtils.isCabinModuleJar(jarFile)) {
                    logger.debug("Found cabin module library: " + library.getFile());
                    library.setScope(LibraryScope.MODULE);
                    moduleLibraries.add(library);
                } else {
                    logger.debug("Found biz library: " + library.getFile());
                    bizLibraries.add(library);
                }
            } catch (Exception e) {
                logger.error("Error while check jar type of " + library.getFile(), e);
            }
        });

        repackageApp();
    }

    private void repackageApp() throws IOException {
        File destination = executableFatJar.getAbsoluteFile();
        if (destination.exists()) {
            destination.delete();
        }

        try (JarWriter writer = new JarWriter(destination)) {
            //write app classes and dependencies
            try (JarFile bizSource = new JarFile(source)) {
                Manifest manifest = buildAppManifest(bizSource);
                writer.writeManifest(manifest);
                writer.writeEntries(bizSource, new RenamingEntryTransformer(APP_CLASSES_DIRECTORY));
                writeNestedLibraries(bizLibraries, APP_LIB_DIRECTORY, writer);
            }

            //write container and modules
            try (JarFile containerSource = new JarFile(containerLibrary.getFile().getAbsoluteFile())) {
                writeConfigFiles(new File(baseDir, CONF_BASE_DIR), writer);
                writer.writeBootstrapEntry(containerSource);
                writeNestedLibraries(Collections.singletonList(containerLibrary), CABIN_CORE_DIRECTORY, writer);
                writeNestedLibraries(moduleLibraries, CABIN_MODULE_DIRECTORY, writer);
            }
        }
    }

    private void writeConfigFiles(final File confDir, final JarWriter jarWriter) throws IOException {
        if (confDir.exists()) {
            final File[] subFiles = confDir.listFiles();
            if (subFiles != null && subFiles.length != 0) {
                for (File subFile : subFiles) {
                    if (subFile.isDirectory()) {
                        writeConfigFiles(subFile, jarWriter);
                    } else {
                        final int index = baseDir.getPath().length();
                        String entryName = subFile.getPath().substring(index);
                        if (entryName.startsWith(File.separator)) {
                            entryName = entryName.substring(1);
                        }
                        jarWriter.writeEntry(FileUtils.getCompatiblePath(entryName), new FileInputStream(subFile));
                    }
                }

            }
        }
    }

    private void writeNestedLibraries(final List<Library> libraries,
                                      final String destination,
                                      final JarWriter writer) throws IOException {
        final Set<String> writeLibs = new HashSet<>();
        for (Library library : libraries) {
            final String libPath = destination + library.getName();
            if (writeLibs.contains(libPath)) {
                throw new IllegalStateException("Duplicate library " + library.getName());
            }
            writeLibs.add(libPath);
            writer.writeNestedLibrary(destination, library);
        }
    }

    /**
     * start main classes
     */
    private Manifest buildAppManifest(JarFile source) throws IOException {
        Manifest manifest = source.getManifest();
        /* theoretically impossible */
        if (manifest == null) {
            manifest = new Manifest();
            manifest.getMainAttributes().putValue(MANIFEST_VERSION, "1.0");
        }
        manifest = new Manifest(manifest);
        manifest.getMainAttributes().putValue(MANIFEST_MAIN_CLASS, CABIN_LAUNCHER_CLASSNAME);
        manifest.getMainAttributes().putValue(MANIFEST_START_CLASS, startClass);
        logger.debug("MainClass: " + CABIN_LAUNCHER_CLASSNAME);
        logger.debug("StartClass: " + startClass);
        if (cabinVersion == null || cabinVersion.isEmpty()) {
            throw new IllegalStateException("must specify version of Cabin.");
        }

        manifest.getMainAttributes().putValue(CABIN_VERSION_ATTRIBUTE, cabinVersion);
        manifest.getMainAttributes().putValue(CABIN_CONTAINER_ROOT, CABIN_CORE_DIRECTORY);
        return manifest;
    }

    private boolean alreadyRepackaged() throws IOException {
        try (JarFile jarFile = new JarFile(this.source)) {
            Manifest manifest = jarFile.getManifest();
            return (manifest != null && manifest.getMainAttributes()
                    .getValue(CABIN_VERSION_ATTRIBUTE) != null);
        }
    }

    public boolean isPackageProvided() {
        return packageProvided;
    }

    public void setPackageProvided(boolean packageProvided) {
        this.packageProvided = packageProvided;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * An {@code EntryTransformer} that renames entries by applying a prefix.
     */
    private static final class RenamingEntryTransformer implements JarWriter.EntryTransformer {

        private final String namePrefix;

        private RenamingEntryTransformer(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public JarEntry transform(JarEntry entry) {
            JarEntry renamedEntry = new JarEntry(this.namePrefix + entry.getName());
            renamedEntry.setTime(entry.getTime());
            renamedEntry.setSize(entry.getSize());
            renamedEntry.setMethod(entry.getMethod());
            if (entry.getComment() != null) {
                renamedEntry.setComment(entry.getComment());
            }
            renamedEntry.setCompressedSize(entry.getCompressedSize());
            renamedEntry.setCrc(entry.getCrc());
            //setCreationTimeIfPossible(entry, renamedEntry);
            if (entry.getExtra() != null) {
                renamedEntry.setExtra(entry.getExtra());
            }
            return renamedEntry;
        }

    }
}

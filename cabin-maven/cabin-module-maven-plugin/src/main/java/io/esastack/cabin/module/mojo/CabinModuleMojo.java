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
package io.esastack.cabin.module.mojo;

import io.esastack.cabin.common.constant.Constants;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.archive.JarFileArchive;
import io.esastack.cabin.loader.util.ArchiveUtils;
import io.esastack.cabin.log.Logger;
import io.esastack.cabin.tools.ArtifactPojo;
import io.esastack.cabin.tools.JarWriter;
import io.esastack.cabin.tools.Repackager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.AbstractZipArchiver;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static io.esastack.cabin.common.constant.Constants.DIRECTORY_SPLITTER;

@Mojo(name = "module-repackage", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CabinModuleMojo extends AbstractMojo {

    private static final String ARCHIVE_MODE = "zip";

    private static final String MODULE_SUFFIX = "_cabin-module.jar";

    private static final String TEMP_MODULE_SUFFIX = MODULE_SUFFIX + ".tmp";

    private final Log log = new Logger(super.getLog());

    @Parameter(defaultValue = "${project.groupId}_${project.artifactId}")
    public String moduleName;

    @Component
    private MavenProject mavenProject;

    @Component
    private ArchiverManager archiverManager;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "${project.build.directory}", property = "cabin.output.directory")
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/tmp-module-dir")
    private File workDirectory;

    @Parameter(defaultValue = "${project.groupId}", readonly = true)
    private String groupId;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String version;

    @Parameter(defaultValue = " ")
    private String description;

    @Parameter(defaultValue = "100", property = "cabin.module.priority")
    private Integer priority;

    @Parameter
    private PropertiesConfig exported;

    @Parameter(defaultValue = "true")
    private boolean loadFromBizClassLoader;

    @Parameter
    private PropertiesConfig imported;

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludes = new LinkedHashSet<>();

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludeGroupIds;

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludeArtifactIds;

    @Parameter(defaultValue = "false")
    private boolean checkSPI;

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludeCheckedSPIs = new LinkedHashSet<>();

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> shadedArtifacts = new LinkedHashSet<>();

    @Parameter(defaultValue = "true")
    private Boolean attach;

    @Parameter(defaultValue = "")
    private String classifier;

    @Override
    public Log getLog() {
        return this.log;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {

        try {
            //Create a zip file without compressing.
            final AbstractZipArchiver archiver;
            try {
                archiver = (AbstractZipArchiver) archiverManager.getArchiver(ARCHIVE_MODE);
                archiver.setCompress(false);
            } catch (NoSuchArchiverException e) {
                throw new MojoExecutionException(e.getMessage());
            }

            getLog().debug("outputDirectory == > " + outputDirectory.getPath());
            getLog().debug("workDirectory == > " + workDirectory.getPath());

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            final File destination = new File(outputDirectory, getFileName());
            final File tmpDestination = new File(outputDirectory, getTempFileName());
            if (destination.exists()) {
                destination.delete();
            }
            if (tmpDestination.exists()) {
                tmpDestination.delete();
            }
            archiver.setDestFile(tmpDestination);

            final Set<Artifact> artifacts = getArtifactsAfterExcluded();
            final Set<Artifact> moduleArtifacts = filterAndGetModuleArtifacts(artifacts);
            final Set<Artifact> providedArtifacts = filterAndGetProvidedArtifacts(artifacts);
            final Set<Artifact> shadedArtifacts = filterAndGetShadedArtifacts(artifacts);
            artifacts.add(mavenProject.getArtifact());

            if (checkSPI) {
                scanAndCheckSpiResources(artifacts);
            }

            appendProvidedClassFile(archiver, providedArtifacts);
            appendArtifacts(archiver, artifacts, Constants.NESTED_LIB_DIRECTORY);
            appendArtifacts(archiver, moduleArtifacts, Constants.NESTED_MODULE_DIRECTORY);
            appendManifest(archiver);
            appendExportClassAndResourceFile(archiver, artifacts);

            try {
                archiver.createArchive();
                constructFinalJar(destination, tmpDestination, shadedArtifacts);
            } catch (ArchiverException | IOException e) {
                getLog().error("Failed to package cabin module!", e);
                throw new MojoExecutionException(e.getMessage());
            } finally {
                tmpDestination.delete();
            }

            if (isAttach()) {
                getLog().info(String.format("Installing file %s to maven repository, classifier is %s",
                        destination.getPath(), classifier));
                if (StringUtils.isEmpty(classifier)) {
                    mavenProject.getArtifact().setFile(destination);
                } else {
                    projectHelper.attachArtifact(mavenProject, destination, classifier);
                }
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public LinkedHashSet<String> getShadedArtifacts() {
        return shadedArtifacts;
    }

    public void setShadedArtifacts(LinkedHashSet<String> shadedArtifacts) {
        this.shadedArtifacts = shadedArtifacts;
    }

    public void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    /**
     * All dependencies of current module can be see by the IDE at coding/compiling time by dependency passing; but for
     * current module, itself is not in the list of dependencies, the IDE cannot see it; so we copy module classes to
     * the top level of the fat jar, and the IDE could see these classes(and resources) while coding, compiling and
     * using without cabin;
     * User can specify some artifact to shade to the top level too, these class will load by LibModuleClassloader.
     */
    private void constructFinalJar(final File moduleFile,
                                  final File tmpModuleFile,
                                  final Set<Artifact> shadedArtifacts) throws IOException {
        shadedArtifacts.add(mavenProject.getArtifact());
        JarWriter writer = new JarWriter(moduleFile);
        try (JarFile tmpJarFile = new JarFile(tmpModuleFile)) {
            writer.writeEntries(tmpJarFile);
            for (Artifact jar : shadedArtifacts) {
                getLog().debug("Copying shaded jar " + jar.getFile() + "to top level.");
                writer.writeEntries(new JarFile(jar.getFile()), jarEntry -> {
                    if (!jarEntry.isDirectory() && !jarEntry.getName().endsWith(Constants.MANIFEST_PATH)) {
                        return jarEntry;
                    }
                    return null;
                });
            }
        } finally {
            writer.close();
        }
    }

    private boolean isShadeJar(Artifact artifact) {
        for (String shade : getShadedArtifacts()) {
            ArtifactPojo artifactPojo = ArtifactPojo.extractArtifactPojo(shade);
            if (!artifact.getGroupId().equals(artifactPojo.getGroupId())) {
                continue;
            }
            if (!artifact.getArtifactId().equals(artifactPojo.getArtifactId())) {
                continue;
            }
            if (!StringUtils.isEmpty(artifactPojo.getClassifier())
                    && !artifactPojo.getClassifier().equals(artifact.getClassifier())) {
                continue;
            }
            if (artifact.getArtifactId().equals(mavenProject.getArtifactId())
                    && artifact.getGroupId().equals(mavenProject.getGroupId())) {
                throw new RuntimeException("Can't shade jar-self.");
            }
            getLog().debug("Shade jar found: " + shade);
            return true;
        }
        return false;
    }

    /**
     * Classes and resources of project jar would extract to top directory for developing and compiling;
     * Project jar is not copied to the libs directory.
     */
    private void appendArtifacts(Archiver archiver, Set<Artifact> artifacts, String nestDirectory) {
        final Set<Artifact> conflictArtifacts = filterArtifactsWithSameArtifactId(artifacts);
        for (Artifact artifact : artifacts) {
            if (Repackager.isZip(artifact.getFile()) && artifact != mavenProject.getArtifact()) {
                appendArtifact(archiver, artifact, nestDirectory, conflictArtifacts.contains(artifact));
            }
        }
    }

    /**
     * Scan JDK SPI and ESA SPI files, all classes in these files are required to be imported, exported, or marked with
     * excludeCheckedSPIs, or else packaging will fail.
     * Why need check?
     * In this condition, Biz and Lib both depend on a third part dependency containing a SPI file, SPI implementations
     * will be loaded bt thread context ClassLoader which usually would be BizModuleClassLoader, so Error happens while
     * the Lib used this implementation.
     * We can check and export/import these classes to avoiding this situation.
     */
    private void scanAndCheckSpiResources(Set<Artifact> artifacts) throws MojoExecutionException {
        getLog().info("excludeCheckedSPIs: " + excludeCheckedSPIs);
        final Map<String, Map<String, Set<String>>> nestSpiClasses = new HashMap<>();
        for (Artifact artifact : artifacts) {
            String artifactPath = artifact.getFile().getPath();
            try {
                JarFileArchive jarFileArchive = new JarFileArchive(artifact.getFile());
                for (final Archive.Entry entry : jarFileArchive) {
                    String entryName = entry.getName();
                    if (!entry.isDirectory() &&
                            (entryName.startsWith(Constants.JDK_SPI_DIRECTORY) ||
                                    entryName.startsWith(Constants.ESA_SPI_DIRECTORY) ||
                                    entryName.startsWith(Constants.ESA_SPI_DIRECTORY_INTERNAL))) {

                        getLog().debug(String.format("SPI resource %s found for %s", entryName, artifactPath));

                        String spiTypeName = entryName.substring(entryName.lastIndexOf(DIRECTORY_SPLITTER) + 1);
                        if (excludeCheckedSPIs != null) {
                            boolean flag = false;
                            for (String excludeCheckedSPI : excludeCheckedSPIs) {
                                if (spiTypeName.startsWith(excludeCheckedSPI)) {
                                    getLog().debug(String.format(
                                            "SPI resource %s is configured as excludeCheckedSPIs", spiTypeName));
                                    flag = true;
                                    break;
                                }
                                getLog().debug(String.format(
                                        "SPI resource %s is not configured as excludeCheckedSPIs", spiTypeName));
                            }
                            if (flag) {
                                continue;
                            }
                        }

                        Set<String> spiClasses = new HashSet<>();
                        spiClasses.add(spiTypeName);
                        URL spiResource = jarFileArchive.getResource(entryName);
                        if (spiResource != null) {
                            try (InputStream inputStream = spiResource.openStream()) {
                                spiClasses.addAll(parseSpiResource(inputStream));
                            } catch (IOException e) {
                                throw new MojoExecutionException(String.format(
                                        "Failed to parse SPI classes from %s of %s", entryName, artifactPath), e);
                            }
                        }

                        getLog().debug(String.format("SPI classes for %s : %s", entryName, spiClasses));

                        for (String className : spiClasses) {
                            if (!isClassExportedOrImported(className)) {
                                Map<String, Set<String>> nestClassMap =
                                        nestSpiClasses.computeIfAbsent(artifactPath, key -> new HashMap<>());
                                Set<String> nestClassSet =
                                        nestClassMap.computeIfAbsent(entryName, key -> new HashSet<>());
                                nestClassSet.add(className);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to scan classes of provided dependencies", e);
            }
        }

        if (!nestSpiClasses.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                    "SPI classes should be configured as [imported|exported|excludeCheckedSPIs]: \r\n");
            nestSpiClasses.forEach((artifactName, artifactClassesMap) -> {
                sb.append("Dependency path: ");
                sb.append(artifactName);
                sb.append("\r\n");
                artifactClassesMap.forEach((entryName, classSet) -> {
                    sb.append("\tSPI resource file: ");
                    sb.append(entryName);
                    sb.append("\t\t SPI classes: ");
                    sb.append(classSet);
                    sb.append("\r\n");
                });
            });
            throw new MojoExecutionException(sb.toString());
        }
    }

    private void appendArtifact(final Archiver archiver,
                                final Artifact artifact,
                                final String nestDirectory,
                                final boolean artifactIdConflict) {
        String destination = artifact.getFile().getName();
        if (artifactIdConflict) {
            destination = artifact.getGroupId() + "-" + destination;
        }
        destination = nestDirectory + destination;
        getLog().debug("Appending artifact: " + artifact + " => " + destination);
        archiver.addFile(artifact.getFile(), destination);
    }

    private Set<String> parseSpiResource(InputStream inputStream) {
        Set<String> spiClasses = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {

                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // Get string before '#'
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    int i = line.indexOf('=');
                    if (i > 0) {
                        line = line.substring(i + 1).trim();
                    }
                    spiClasses.add(line);
                }
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Load extension class catch an exception: " + t.getMessage(), t);
        }

        return spiClasses;
    }

    private boolean isClassExportedOrImported(final String className) {
        for (PropertiesConfig config: new PropertiesConfig[]{exported, imported}) {
            if (config.getClasses() != null && config.getClasses().contains(className)) {
                return true;
            }

            if (config.getPackages() != null) {
                for (String pkg : config.getPackages()) {
                    if (className.startsWith(pkg)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * if some dependencies have same artifactId, use groupId + artifactID as the file name in fat jar
     */
    private Set<Artifact> filterArtifactsWithSameArtifactId(final Set<Artifact> artifacts) {
        final Map<String, Artifact> existArtifacts = new HashMap<>();
        final Set<Artifact> result = new HashSet<>();
        for (Artifact artifact : artifacts) {
            final String artifactId = artifact.getArtifactId();
            if (existArtifacts.containsKey(artifactId)) {
                result.add(artifact);
                result.add(existArtifacts.get(artifactId));
            } else {
                existArtifacts.put(artifactId, artifact);
            }
        }
        return result;
    }

    /**
     * Support this config format: 'esa:servicekeeper-*':[classifier]
     */
    private Set<Artifact> getArtifactsAfterExcluded() {
        final List<ArtifactPojo> excludedList = new ArrayList<>();
        for (String exclude : excludes) {
            ArtifactPojo item = ArtifactPojo.extractArtifactPojo(exclude);
            excludedList.add(item);
        }

        Set<Artifact> result = new LinkedHashSet<>();
        for (Artifact e : (Set<Artifact>) mavenProject.getArtifacts()) {
            boolean isExclude = false;
            for (ArtifactPojo exclude : excludedList) {
                String artifactPrefix = exclude.getArtifactId();
                int index = artifactPrefix.indexOf("*");
                if (index > 0) {
                    artifactPrefix = artifactPrefix.substring(0, index);
                }
                if (StringUtils.equals(exclude.getGroupId(), e.getGroupId()) &&
                        StringUtils.equals(exclude.getClassifier(), e.getClassifier()) &&
                        e.getArtifactId().startsWith(artifactPrefix)) {
                    isExclude = true;
                    break;
                }
            }

            if (excludeGroupIds != null && excludeGroupIds.contains(e.getGroupId())) {
                isExclude = true;
            }

            if (excludeArtifactIds != null && excludeArtifactIds.contains(e.getArtifactId())) {
                isExclude = true;
            }

            if (!isExclude) {
                result.add(e);
                getLog().debug("Dependency: " + e.toString() + " is included!");
            } else {
                getLog().debug("Dependency: " + e.toString() + " is excluded!");
            }
        }

        return result;
    }

    private Set<Artifact> filterAndGetProvidedArtifacts(Set<Artifact> artifacts) {
        Set<Artifact> result = new LinkedHashSet<>();
        Iterator<Artifact> iterator = artifacts.iterator();
        while (iterator.hasNext()) {
            Artifact artifact = iterator.next();
            if (artifact.getScope().equals(Artifact.SCOPE_PROVIDED) ||
                    artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
                result.add(artifact);
                iterator.remove();
            }
        }

        return result;
    }

    private Set<Artifact> filterAndGetShadedArtifacts(Set<Artifact> artifacts) {
        Set<Artifact> result = new LinkedHashSet<>();
        Iterator<Artifact> iterator = artifacts.iterator();
        while (iterator.hasNext()) {
            Artifact artifact = iterator.next();
            if (isShadeJar(artifact)) {
                result.add(artifact);
                iterator.remove();
            }
        }

        return result;
    }

    private Set<Artifact> filterAndGetModuleArtifacts(Set<Artifact> artifacts) throws MojoExecutionException {
        Set<Artifact> normalArtifacts = new LinkedHashSet<>();
        Set<Artifact> moduleArtifacts = new LinkedHashSet<>();
        for (Artifact artifact : artifacts) {
            try (JarFile jarFile = new JarFile(artifact.getFile())) {
                if (ArchiveUtils.isCabinModuleJar(jarFile)) {
                    if (artifact.isOptional()) {
                        getLog().debug(String.format("Cabin Module %s is depended by module %s, and is optional, " +
                                        "put it into modules/ directory",
                                getArtifactIdentity(artifact),
                                getArtifactIdentity(mavenProject.getArtifact())));
                        moduleArtifacts.add(artifact);
                    } else {
                        getLog().debug(String.format("Cabin Module %s is depended by module %s, and is not optional, " +
                                        "not put it into modules/ directory",
                                getArtifactIdentity(artifact),
                                getArtifactIdentity(mavenProject.getArtifact())));
                    }
                } else {
                    normalArtifacts.add(artifact);
                }
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Failed to filter module artifact :" + getArtifactIdentity(artifact), e);
            }
        }
        artifacts.clear();
        artifacts.addAll(normalArtifacts);
        return moduleArtifacts;
    }

    private String getArtifactIdentity(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    private String getFileName() {
        return String.format("%s%s", moduleName, MODULE_SUFFIX);
    }

    private String getTempFileName() {
        return String.format("%s%s", moduleName, TEMP_MODULE_SUFFIX);
    }

    private boolean isAttach() {
        return attach;
    }

    private void appendExportClassAndResourceFile(final Archiver archiver,
                                                  final Set<Artifact> artifacts) throws MojoExecutionException {

        Set<String> resources = new HashSet<>();
        Set<String> resourcePrefixs = new HashSet<>();
        Set<String> exportClasses = new HashSet<>();
        Set<String> exportPackages = new HashSet<>();

        resolveExportClasses(artifacts, exportClasses, exportPackages);
        resolveExportResources(artifacts, resources, resourcePrefixs);
        scanClassesAndResourcesFromArchives(artifacts, exportPackages, exportClasses, resourcePrefixs, resources);
        appendStringCollectionToFile(archiver, Constants.EXPORTED_CLASS_FILE, exportClasses);
        appendStringCollectionToFile(archiver, Constants.EXPORTED_RESOURCE_FILE, resources);

    }

    private void appendProvidedClassFile(Archiver archiver, Set<Artifact> artifacts) throws MojoExecutionException {
        List<String> providedClasses = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            try {
                JarFileArchive jarFileArchive = new JarFileArchive(artifact.getFile());
                jarFileArchive.getNestedArchives(entry -> {
                    String entryName = entry.getName();
                    if (entryName.endsWith(Constants.CLASS_FILE_SUFFIX)) {
                        providedClasses.add(
                                entryName.substring(0, entryName.length() - 6).replace("/", "."));
                    }
                    return false;
                });
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to scan classes of provided dependencies", e);
            }
        }

        appendStringCollectionToFile(archiver, Constants.PROVIDED_CLASS_FILE, providedClasses);
    }

    private void resolveExportClasses(Set<Artifact> artifacts, Set<String> exportClasses, Set<String> exportPackages) {
        exportClasses.addAll(exported.getClasses());
        Set<String> packages = exported.getPackages();
        if ((CollectionUtils.isEmpty(exportClasses) && CollectionUtils.isEmpty(packages)) ||
                CollectionUtils.isEmpty(artifacts)) {
            getLog().debug("Scanning Export classes: " + "empty export packages and classes");
            return;
        }

        for (String pkg : packages) {
            if (StringUtils.isBlank(pkg)) {
                continue;
            }
            getLog().debug("Scanning Export classes: package ==>" + pkg);
            exportPackages.add(pkg.trim().replace(".", "/"));
        }
    }

    private void resolveExportResources(Set<Artifact> artifacts, Set<String> resources, Set<String> resourcePrefixs) {
        Set<String> exportResources = new HashSet<>(exported.getResources());
        if (CollectionUtils.isEmpty(exportResources) || CollectionUtils.isEmpty(artifacts)) {
            getLog().debug("Scanning Export resources: " + "empty export resources");
            return;
        }

        for (String resource : exportResources) {
            if (StringUtils.isBlank(resource)) {
                continue;
            }
            resource = resource.trim();
            getLog().debug("Scanning Export resources: resource ==>" + resource);
            if (resource.endsWith(DIRECTORY_SPLITTER)) {
                resourcePrefixs.add(resource);
            } else if (resource.endsWith(Constants.CHARACTER_ANY)) {
                while (resource.endsWith(Constants.CHARACTER_ANY)) {
                    resource = resource.substring(0, resource.length() - 1);
                }
                if (resource.length() > 0) {
                    resourcePrefixs.add(resource);
                }
            } else {
                resources.add(resource);
            }
        }
    }

    /**
     * Scan all the artifacts to get all the expored classes and resources;
     * Classes in spring.factories are exported as classes and resources automatic
     */
    private void scanClassesAndResourcesFromArchives(Set<Artifact> artifacts,
                                                     Set<String> exportPackages,
                                                     Set<String> exportClasses,
                                                     Set<String> resourcePrefixs,
                                                     Set<String> exportResources) throws MojoExecutionException {
        getLog().debug("Begin to scan artifacts to get exported classes and resources, " +
                "packages: " + StringUtils.join(exportPackages, ",") +
                "; resources: " + StringUtils.join(exportResources, ","));
        for (Artifact artifact : artifacts) {
            JarFileArchive archive;
            try {
                archive = new JarFileArchive(artifact.getFile());
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to create JarFileArchive from: " + artifact.getFile().getPath(), e);
            }
            getLog().debug("Scanning Export classes from artifact ==>" + artifact.getFile().getPath());
            try {
                archive.getNestedArchives(entry -> {
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        for (String exportPackage : exportPackages) {
                            if (entryName.startsWith(exportPackage) && entryName.endsWith(".class")) {
                                exportClasses.add(entryName.substring(0, entryName.length() - 6)
                                        .replace("/", "."));
                                getLog().debug("Scanning Export classes from artifact ==>" +
                                        artifact.getFile().getPath() + ", entry: " + entry.getName());
                            }
                        }
                        for (String resource : resourcePrefixs) {
                            if (entryName.startsWith(resource)) {
                                exportResources.add(entryName);
                            }
                        }
                    }
                    return false;
                });

                final URL springFactoriesUrl = archive.getResource(Constants.SPRINGBOOT_FACTORIES);
                final List<String> classes = getSpringbootSpiImpls(springFactoriesUrl);
                if (CollectionUtils.isNotEmpty(classes)) {
                    exportClasses.addAll(classes);
                    classes.forEach(clz -> exportResources.add(clz.replace(".", "/") + ".class"));
                }
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to get nested archives from JarFileArchive: " + artifact.getFile().getPath(), e);
            }
        }
    }

    private void appendStringCollectionToFile(final Archiver archiver,
                                              final String dstFile,
                                              final Collection<String> exported) throws MojoExecutionException {
        final List<String> result = new ArrayList<>(exported);
        Collections.sort(result);
        final File file = new File(this.workDirectory + "/" + dstFile);
        if (file.exists()) {
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileWriter fileWriter = null;
        try {
            file.createNewFile();
            fileWriter = new FileWriter(file);
            for (String resource : result) {
                fileWriter.append(resource);
                fileWriter.append("\n");
            }
            fileWriter.flush();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create new File: " + file.getPath(), e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    //NOP
                }
            }
        }
        archiver.addFile(file, dstFile);
    }

    private void appendManifest(Archiver archiver) throws MojoExecutionException {
        Map<String, String> properties = new HashMap<>();

        properties.put(Constants.MANIFEST_MODULE_GROUP_ID, groupId);
        properties.put(Constants.MANIFEST_MODULE_ARTIFACT_ID, artifactId);
        properties.put(Constants.MANIFEST_MODULE_VERSION, version);
        properties.put(Constants.MANIFEST_MODULE_PRIORITY, String.valueOf(priority));
        properties.put(Constants.MANIFEST_MODULE_NAME, moduleName);
        properties.put(Constants.MANIFEST_MODULE_DESC, description);
        properties.put(Constants.MANIFEST_LOAD_FROM_BIZ, String.valueOf(loadFromBizClassLoader));
        properties.putAll(collectImportInfo());
        properties.putAll(collectExportInfo());

        File file = new File(workDirectory.getPath() + File.separator + Constants.MANIFEST_PATH);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        PrintStream outputStream = null;
        //Manifest manifest = new LinkedManifest();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        getLog().debug("Properties size:" + properties.size());
        for (String key : properties.keySet()) {
            String value = properties.get(key);
            getLog().debug("KV pairs to write to MANIFEST: key=" + key + "; value=" + value);
            manifest.getMainAttributes().putValue(key, value);
        }

        try {
            outputStream = new PrintStream(file, "UTF-8");
            manifest.write(outputStream);
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage());
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }

        archiver.addFile(file, Constants.MANIFEST_PATH);
    }

    private Map<String, String> collectExportInfo() {
        Map<String, String> properties = new HashMap<>();
        if (exported == null) {
            exported = new PropertiesConfig();
        }
        properties.put("export-packages", exported.getPackagesString());
        properties.put("export-classes", exported.getClassesString());
        properties.put("export-resources", exported.getResourcesString());
        return properties;
    }

    private Map<String, String> collectImportInfo() {
        Map<String, String> properties = new HashMap<>();
        if (imported == null) {
            imported = new PropertiesConfig();
        }
        properties.put("import-packages", imported.getPackagesString());
        properties.put("import-classes", imported.getClassesString());
        properties.put("import-resources", imported.getResourcesString());
        return properties;
    }

    /**
     * Package access for unit test.
     * Parse the SPI implementation class names of 'spring.factories' file; these classes will be added into exported
     * resources and exported classes in order to:
     * 1. These classes will be loaded by BizModuleClassLoader, so they must be exported;
     * 2. Auto Configuration classed used with 'org.springframework.boot.autoconfigure.EnableAutoConfiguration'
     *    will be parsed by ASM to get meta data from class file(bytes code), class file will be searched by
     *    BizModuleClassLoader::getResource(), so the class file must be exported too;
     * 3. Other SPI implementation class files are exported by the way.
     */
    List<String> getSpringbootSpiImpls(final URL springFactoriesUrl) throws MojoExecutionException {
        final List<String> result = new ArrayList<>();
        if (springFactoriesUrl == null) {
            return result;
        }

        getLog().debug("Found spring.factories from: " + springFactoriesUrl.toExternalForm());
        try (final BufferedReader bufferedReader =
                     new BufferedReader(new InputStreamReader(springFactoriesUrl.openStream()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    line = line.replace("\\", "");
                    line = line.trim();
                    if (line.startsWith("#")) {
                        continue;
                    }
                    int index = line.indexOf("=");
                    if (index > 0) {
                        line = line.substring(index + 1).trim();
                    }
                    if (!StringUtils.isBlank(line)) {
                        String[] clazzArray = line.split(",");
                        for (String clazz : clazzArray) {
                            if (StringUtils.isNotBlank(clazz)) {
                                result.add(clazz.trim());
                            }
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            throw new MojoExecutionException(
                    "Failed to parse spring.factories from: " + springFactoriesUrl.toExternalForm());
        }

        return result;

    }

}

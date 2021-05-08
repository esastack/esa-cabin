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
package io.esastack.cabin.boot.mojo;

import io.esastack.cabin.log.Logger;
import io.esastack.cabin.tools.ArtifactPojo;
import io.esastack.cabin.tools.Libraries;
import io.esastack.cabin.tools.Repackager;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CabinAppMojo extends AbstractMojo {

    private final Log log = new Logger(super.getLog());

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenProjectHelper mavenProjectHelper;

    @Component
    private MavenSession mavenRepoSession;

    @Component
    private ArtifactFactory mavenArtifactFactory;

    @Component
    private ArtifactResolver mavenArtifactResolver;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDir;

    @Parameter(defaultValue = "${project.basedir}", required = true)
    private File baseDir;

    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    @Parameter(readonly = true)
    private String classifier;

    @Parameter(defaultValue = "false")
    private boolean attach;

    @Parameter(required = true)
    private String mainClass;

    @Parameter
    private List<Dependency> requiresUnpack;

    /**
     * The version of EsaCabin, same with module version.
     */
    private String cabinVersion;

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludes = new LinkedHashSet<>();

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludeGroupIds = new LinkedHashSet<>();

    @Parameter(defaultValue = "")
    private LinkedHashSet<String> excludeArtifactIds = new LinkedHashSet<>();

    @Parameter(defaultValue = "false")
    private boolean packageProvided;

    @Parameter(defaultValue = "")
    private String extension;

    @Override
    public Log getLog() {
        return this.log;
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            final String packaging = this.mavenProject.getPackaging();
            if (!"jar".equals(packaging)) {
                getLog().debug("repackage goal could only be applied to jar project, this project is: " + packaging);
                return;
            }
            this.cabinVersion = ((PluginDescriptor) getPluginContext().get("pluginDescriptor")).getVersion();
            doRepackage();
        } catch (Throwable ex) {
            getLog().error("Failed to repackage cabin application!", ex);
            throw ex;
        }
    }

    private void doRepackage() throws MojoExecutionException {
        final File executableTargetFile = getExecutableTargetFile();
        final Repackager repackager = getAppRepackager();
        final Libraries libraries = new ArtifactsLibraries(getArtifactsWithCabinCore(), this.requiresUnpack, getLog());
        try {
            repackager.repackage(executableTargetFile, libraries);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        updateArtifact(executableTargetFile);
    }

    private File saveAndGetOriginalProjectFile() throws MojoExecutionException {
        final File projectFile = this.mavenProject.getArtifact().getFile();
        final File original = new File(projectFile.getAbsolutePath() + ".original");
        if (original.exists()) {
            original.delete();
        }
        try {
            Files.copy(projectFile.toPath(), original.toPath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed copy source file to original", ex);
        }
        return original;
    }

    /**
     * If the cabin-core artifact is not in the dependencies, resolve it from maven repository.
     */
    @SuppressWarnings("unchecked")
    private Set<Artifact> getArtifactsWithCabinCore() throws MojoExecutionException {
        Set<Artifact> dependencies = filterExcludeArtifacts(mavenProject.getArtifacts());
        boolean resolveCabinCore = true;
        for (Artifact artifact : dependencies) {
            if (artifact.getGroupId().equals(CabinConstants.groupId) &&
                    artifact.getArtifactId().equals(CabinConstants.cabinCoreArtifactId) &&
                    artifact.getScope().endsWith(CabinConstants.scope) &&
                    artifact.getType().equals(CabinConstants.type)) {
                resolveCabinCore = false;
                break;
            }
        }
        if (resolveCabinCore) {
            try {
                Artifact cabinCoreArtifact = mavenArtifactFactory.createArtifact(CabinConstants.groupId,
                        CabinConstants.cabinCoreArtifactId, cabinVersion, CabinConstants.scope, CabinConstants.type);
                mavenArtifactResolver.resolve(cabinCoreArtifact, mavenProject.getRemoteArtifactRepositories(),
                        mavenRepoSession.getLocalRepository());
                dependencies.add(cabinCoreArtifact);
            } catch (Exception ex) {
                throw new MojoExecutionException("Failed to find container archive: " + ex.getMessage(), ex);
            }
        }
        return dependencies;
    }

    private File getExecutableTargetFile() {
        String classifier = (this.classifier == null ? "" : this.classifier.trim());
        if (classifier.length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }
        if (!this.outputDir.exists()) {
            this.outputDir.mkdirs();
        }

        final String ext = StringUtils.isBlank(this.extension)
                ? this.mavenProject.getArtifact().getArtifactHandler().getExtension() : this.extension;
        return new File(this.outputDir, this.finalName + classifier + "." + ext);
    }

    private Repackager getAppRepackager() throws MojoExecutionException {
        final File original = saveAndGetOriginalProjectFile();
        Repackager repackager = new Repackager(original, getLog());
        repackager.setCabinVersion(cabinVersion);
        repackager.setPackageProvided(packageProvided);
        repackager.setBaseDir(baseDir);
        repackager.setStartClass(mainClass);
        return repackager;
    }

    private void updateArtifact(File repackaged) {
        if (this.attach) {
            attachArtifact(repackaged, classifier);
        }
    }

    private void attachArtifact(File jarFile, String classifier) {
        getLog().info("Attaching archive:" + jarFile + ", with classifier: " + classifier);
        this.mavenProjectHelper.attachArtifact(mavenProject, mavenProject.getPackaging(), classifier, jarFile);
    }

    /**
     * cabin artifacts are excluded, except cabin-core
     */
    protected Set<Artifact> filterExcludeArtifacts(Set<Artifact> artifacts) {
        List<ArtifactPojo> excludeList = new ArrayList<>();
        for (String exclude : excludes) {
            ArtifactPojo item = ArtifactPojo.extractArtifactPojo(exclude);
            excludeList.add(item);
            getLog().debug("Exclude artifact is configured: " + item.toString());
        }

        for (String excludeGroupId : excludeGroupIds) {
            getLog().debug("Exclude groupId is configured: " + excludeGroupId);
        }

        for (String excludeArtifactId : excludeArtifactIds) {
            getLog().debug("Exclude artifactId is configured: " + excludeArtifactId);
        }

        Set<Artifact> result = new LinkedHashSet<>();
        for (Artifact e : artifacts) {

            getLog().debug("Dependency artifactId: " + e.toString());

            boolean isExclude = false;
            for (ArtifactPojo exclude : excludeList) {
                if (exclude.isSameArtifact(ArtifactPojo.extractArtifactPojo(e))) {
                    isExclude = true;
                    break;
                }
            }

            if (excludeGroupIds.contains(e.getGroupId()) || excludeArtifactIds.contains(e.getArtifactId())) {
                isExclude = true;
            }

            if (e.getGroupId().equals(CabinConstants.groupId) &&
                    CabinConstants.cabinArtifacts.contains(e.getArtifactId()) &&
                    !e.getArtifactId().equals(CabinConstants.cabinCoreArtifactId)) {
                isExclude = true;
            }

            if (!isExclude) {
                result.add(e);
            } else {
                getLog().debug("Exclude artifact: " + e.toString());
            }
        }

        return result;
    }

    public static class CabinConstants {

        private static String groupId = "io.esastack";

        private static String cabinCoreArtifactId = "cabin-core";

        private static String scope = "compile";

        private static String type = "jar";

        private static Set<String> cabinArtifacts = new HashSet<>(Arrays.asList("cabin-api",
                "cabin-archive",
                "cabin-boot",
                "cabin-bootstrap",
                "cabin-common",
                "cabin-container",
                "cabin-maven",
                "cabin-maven-common",
                "cabin-maven-plugin",
                "cabin-module-maven-plugin",
                "cabin-test"));
    }

}

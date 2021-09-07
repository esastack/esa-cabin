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
package io.esastack.cabin.boot.mojo;

import io.esastack.cabin.tools.Libraries;
import io.esastack.cabin.tools.Library;
import io.esastack.cabin.tools.LibraryCallback;
import io.esastack.cabin.tools.LibraryScope;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.*;

public class ArtifactsLibraries implements Libraries {

    private static final Map<String, LibraryScope> SCOPES;

    static {
        Map<String, LibraryScope> libraryScopes = new HashMap<>();
        libraryScopes.put(Artifact.SCOPE_COMPILE, LibraryScope.COMPILE);
        libraryScopes.put(Artifact.SCOPE_RUNTIME, LibraryScope.RUNTIME);
        libraryScopes.put(Artifact.SCOPE_PROVIDED, LibraryScope.PROVIDED);
        libraryScopes.put(Artifact.SCOPE_SYSTEM, LibraryScope.PROVIDED);
        SCOPES = Collections.unmodifiableMap(libraryScopes);
    }

    private final Set<Artifact> artifacts;

    private final Collection<Dependency> unpacks;

    private final Log logger;

    public ArtifactsLibraries(Set<Artifact> artifacts, Collection<Dependency> unpacks, Log logger) {
        this.artifacts = artifacts;
        this.unpacks = unpacks;
        this.logger = logger;
    }

    @Override
    public void doWithLibraries(LibraryCallback callback) throws IOException {
        Set<String> duplicates = getDuplicates(artifacts);
        for (Artifact artifact : this.artifacts) {
            LibraryScope scope = SCOPES.get(artifact.getScope());
            if (scope != null && artifact.getFile() != null) {
                String name = getFileName(artifact);
                if (duplicates.contains(name)) {
                    String oldName = name;
                    name = artifact.getGroupId() + "-" + oldName;
                    logger.debug(String.format("Duplicate found: %s, renamed to: %s", oldName, name));
                }
                callback.library(new Library(name, artifact.getFile(), scope,
                        isUnpackRequired(artifact)));
            } else {
                logger.debug(String.format("Ignore invalid artifact: %s, scope is: %s, " +
                        "artifact file is: %s", artifact.toString(), scope, artifact.getFile()));
            }
        }
    }

    private Set<String> getDuplicates(Set<Artifact> artifacts) {
        Set<String> duplicates = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (Artifact artifact : artifacts) {
            String fileName = getFileName(artifact);
            if (artifact.getFile() != null && !seen.add(fileName)) {
                duplicates.add(fileName);
            }
        }
        return duplicates;
    }

    private boolean isUnpackRequired(Artifact artifact) {
        if (this.unpacks != null) {
            for (Dependency unpack : this.unpacks) {
                if (artifact.getGroupId().equals(unpack.getGroupId())
                        && artifact.getArtifactId().equals(unpack.getArtifactId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getFileName(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getArtifactId()).append("-").append(artifact.getBaseVersion());
        String classifier = artifact.getClassifier();
        if (classifier != null) {
            sb.append("-").append(classifier);
        }
        sb.append(".").append(artifact.getArtifactHandler().getExtension());
        return sb.toString();
    }

}

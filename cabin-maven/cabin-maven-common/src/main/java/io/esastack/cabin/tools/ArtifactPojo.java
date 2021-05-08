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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;

public class ArtifactPojo {

    private String groupId;

    private String artifactId;

    private String version;

    private String classifier;

    /**
     * @param format  groupId:artifactId or groupId:artifactId:classifier
     */
    public static ArtifactPojo extractArtifactPojo(final String format) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("Format should not be empty!");
        }

        final String[] arr = StringUtils.split(format, ":");
        if (arr.length < 2 || arr.length > 3) {
            throw new IllegalArgumentException(String.format("Invalid artifact format: %s", format));
        }

        final ArtifactPojo pojo = new ArtifactPojo();
        pojo.setGroupId(arr[0]);
        pojo.setArtifactId(arr[1]);
        if (arr.length == 3) {
            pojo.setClassifier(arr[2]);
        }
        return pojo;
    }

    public static ArtifactPojo extractArtifactPojo(final Artifact artifact) {
        ArtifactPojo pojo = new ArtifactPojo();
        pojo.setGroupId(artifact.getGroupId());
        pojo.setArtifactId(artifact.getArtifactId());
        pojo.setClassifier(artifact.getClassifier());
        pojo.setVersion(artifact.getVersion());
        return pojo;
    }

    public boolean isSameArtifact(ArtifactPojo pojo) {
        if (pojo == null) {
            return false;
        }
        return StringUtils.equals(this.getGroupId(), pojo.getGroupId())
                && StringUtils.equals(this.getArtifactId(), pojo.getArtifactId())
                && StringUtils.equals(this.getClassifier(), pojo.getClassifier());
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String toString() {
        return String.format("%s:%s:%s:%s", groupId, artifactId, classifier, version);
    }
}

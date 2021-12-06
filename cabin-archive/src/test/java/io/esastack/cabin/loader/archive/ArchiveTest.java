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
package io.esastack.cabin.loader.archive;

import io.esastack.cabin.loader.util.ArchiveUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class ArchiveTest {

    @Test
    public void expoldedArchiveTest() throws Exception {
        final URL url = ArchiveTest.class.getClassLoader().getResource("");
        final Archive archive = ArchiveUtils.createArchiveFromUrl(url);

        Assert.assertTrue(archive instanceof ExplodedArchive);

        URL rs = archive.getResource(null);
        Assert.assertNull(rs);
        rs = archive.getResource("test");
        Assert.assertNull(rs);
        rs = archive.getResource("resource.file");
        Assert.assertNotNull(rs);


        Manifest manifest = archive.getManifest();
        Assert.assertNotNull(manifest);
        Assert.assertNotNull(archive.toString());

        final ExplodedArchive explodedArchive = (ExplodedArchive) archive;
        Assert.assertFalse(explodedArchive.getNestedArchives().isEmpty());
        Assert.assertNotNull(explodedArchive.getUrl());
        explodedArchive.forEach(entry -> {
            try {
                if (entry instanceof JarEntry && ((JarEntry) entry).getMethod() == ZipEntry.STORED) {
                    Assert.assertTrue(explodedArchive.getNestedArchive(entry) != null);
                    Assert.assertNotNull(explodedArchive.getResource(entry.getName()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail();
            }
        });
    }

    @Test
    public void jarFileArchiveTest() throws Exception {
        final URL url = ArchiveTest.class.getClassLoader().getResource("main-cabin-executable.jar");
        final Archive archive = ArchiveUtils.createArchiveFromUrl(url);

        Assert.assertTrue(archive instanceof JarFileArchive);

        URL rs = archive.getResource(null);
        Assert.assertNull(rs);
        rs = archive.getResource("test");
        Assert.assertNull(rs);
        rs = archive.getResource("APP-INF/classes/logback.xml");
        Assert.assertNotNull(rs);
        Assert.assertNotNull(archive.toString());

        final JarFileArchive jarFileArchive = (JarFileArchive) archive;
        Assert.assertFalse(jarFileArchive.getNestedArchives().isEmpty());
        Assert.assertNotNull(jarFileArchive.getManifest());
        Assert.assertNotNull(jarFileArchive.getUrl());
        jarFileArchive.forEach(entry -> {
            try {
                if (entry instanceof JarEntry && ((JarEntry) entry).getMethod() == ZipEntry.STORED) {
                    Assert.assertTrue(jarFileArchive.getNestedArchive(entry) != null);
                    Assert.assertNotNull(jarFileArchive.getResource(entry.getName()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail();
            }
        });
    }

}

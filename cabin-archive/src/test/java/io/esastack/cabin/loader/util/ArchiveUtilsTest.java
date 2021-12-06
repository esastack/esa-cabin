package io.esastack.cabin.loader.util;

import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.archive.ArchiveTest;
import io.esastack.cabin.loader.archive.JarFileArchive;
import io.esastack.cabin.loader.jar.JarFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.net.URL;

public class ArchiveUtilsTest {

    @Test
    public void archiveTest() throws Exception {

        try {
            ArchiveUtils.getFile(null);
            Assert.fail();
        } catch (Exception e) {
        }
        URL resourceUrl = null;
        try {
            resourceUrl = new URL("TEST://hshsh");
            Assert.assertNull(ArchiveUtils.getFile(resourceUrl));
            Assert.fail();
        } catch (Exception e) {
        }

        resourceUrl = ArchiveTest.class.getClassLoader().getResource("main-cabin-executable.jar");
        final Archive archive = ArchiveUtils.createArchiveFromUrl(resourceUrl);
        Archive containerUrl = ArchiveUtils.extractContainerArchive(archive);
        URL[] moduleUrls = ArchiveUtils.extractBizUrls(archive);
        URL[] bizUrls = ArchiveUtils.extractBizUrls(archive);
        Assert.assertNotNull(containerUrl);
        Assert.assertNotNull(moduleUrls);
        Assert.assertNotNull(bizUrls);
        Assert.assertTrue(ArchiveUtils.isCabinContainerJar(((JarFileArchive) containerUrl).getJarFile()));
        Assert.assertFalse(ArchiveUtils.isCabinModuleJar(((JarFileArchive) containerUrl).getJarFile()));
    }
}

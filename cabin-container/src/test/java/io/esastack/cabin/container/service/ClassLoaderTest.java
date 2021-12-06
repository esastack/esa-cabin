package io.esastack.cabin.container.service;

import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.container.service.loader.JavaAgentClassLoader;
import io.esastack.cabin.container.service.loader.UnitTestModuleClassLoader;
import org.junit.Assert;
import org.junit.Test;

public class ClassLoaderTest {

    @Test
    public void unitTestModuleClassLoaderTest() throws Exception {
        UnitTestModuleClassLoader loader = new UnitTestModuleClassLoader(ClassLoaderUtils.getSystemClassPaths());
        Assert.assertNotNull(loader.loadClass("org.junit.Test"));
        Assert.assertNotNull(loader.loadClass("io.esastack.cabin.container.service.ClassLoaderTest"));
    }

    @Test
    public void JavaAgentClassLoaderTest() throws Exception {
        JavaAgentClassLoader classLoader =
                new JavaAgentClassLoader(ClassLoader.getSystemResource("cabin-sample-app-0.1.0.jar"));
        Assert.assertNotNull(classLoader.getAgentUrl());
    }
}

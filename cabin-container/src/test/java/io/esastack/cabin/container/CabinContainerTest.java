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
package io.esastack.cabin.container;

import io.esastack.cabin.common.constant.Constants;
import io.esastack.cabin.common.util.CabinContainerUtil;
import io.esastack.cabin.common.util.ClassLoaderUtils;
import io.esastack.cabin.container.service.loader.BizModuleClassLoader;
import io.esastack.cabin.container.service.loader.LibModuleClassLoader;
import io.esastack.cabin.loader.archive.Archive;
import io.esastack.cabin.loader.util.ArchiveUtils;
import io.esastack.cabin.support.boot.launcher.CabinJarLauncher;
import io.esastack.cabin.support.bootstrap.CabinClasspathLauncher;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class CabinContainerTest {

    private static final URL executableJarArchive = Thread.currentThread().getContextClassLoader()
            .getResource("cabin-sample-app-0.1.0.jar");

    private static final URL agentJarArchive = Thread.currentThread().getContextClassLoader()
            .getResource("cabin-sample-agent-0.1.0.jar");

    private static final URL cabinCoreArchive = Thread.currentThread().getContextClassLoader()
            .getResource("cabin-core-0.1.0-SNAPSHOT.jar");

    private static final String mainClass = "io.esastack.cabin.sample.app.CabinTestApp";

    private static final String mainMethod = "main";

    static {
        System.setProperty(Constants.CABIN_LOG_LEVEL, "DEBUG");
    }

    @Test
    public void containerTest() throws Exception {
        final CabinContainer cpContainer = new CabinContainer(cabinCoreArchive.toExternalForm(),
                new String[0], new String[0], new String[]{agentJarArchive.toExternalForm()},
                new String[]{mainClass, mainMethod});
        Assert.assertNotNull(cpContainer);

        final CabinContainer container = new CabinContainer(executableJarArchive.toExternalForm(),
                new String[]{agentJarArchive.toExternalForm()}, new String[]{mainClass, mainMethod});
        container.start();

        Assert.assertTrue(container.isStarted());
        Assert.assertFalse(container.getExportedClasses().isEmpty());
        Assert.assertNotNull(container.getBizModuleClassLoader());
        Assert.assertTrue(container.getBizModuleClassLoader() instanceof BizModuleClassLoader);
        Assert.assertEquals(2, container.getLoadedModule().size());

        LibModuleClassLoader libModuleClassLoader =
                (LibModuleClassLoader) container.getLibModuleClassLoader("io.esastack_cabin-sample-lib-module");
        Assert.assertNotNull(libModuleClassLoader);

        Assert.assertNotNull(libModuleClassLoader.loadClass("io.esastack.cabin.container.domain.BizModule"));
        Assert.assertNotNull(libModuleClassLoader.loadClass("io.esastack.cabin.sample.lib.module.CabinTestLibModule"));
        Assert.assertNotNull(libModuleClassLoader.loadClass("io.esastack.cabin.sample.app.CabinTestApp"));
        Assert.assertNotNull(libModuleClassLoader.getResource("export.file"));
        Assert.assertNotNull(libModuleClassLoader.getResources("export.file"));

        final String moduleName = container.getLoadedModule().get(0);
        Assert.assertTrue(container.moduleLoaded(moduleName));
        Assert.assertTrue(container.getLibModuleClassLoader(moduleName) instanceof LibModuleClassLoader);
        Assert.assertTrue(CabinContainerUtil.isStarted());
        Assert.assertTrue(CabinContainerUtil.moduleLoaded(moduleName));
        Assert.assertTrue(CabinContainerUtil.getBizClassLoader() instanceof BizModuleClassLoader);

        final Enumeration<URL> filters = container.getBizModuleClassLoader()
                .getResources("export.file");
        Assert.assertTrue(filters.hasMoreElements());
        container.stop();
    }

    @Test
    public void jarLauncherTest() throws Exception {
        final Archive archive = ArchiveUtils.createArchiveFromUrl(executableJarArchive);
        final FacadeCabinJarLauncher jarLauncher = new FacadeCabinJarLauncher(archive);
        jarLauncher.launch(new String[0]);
    }

    @Test
    public void classpathLauncherTest() throws Exception {
        final List<URL> urls = new ArrayList<>(Arrays.asList(ClassLoaderUtils.getSystemClassPaths()));
        final URL cabinCoreUrl = CabinContainerTest.class.getClassLoader()
                .getResource("cabin-core-0.1.0-SNAPSHOT.jar");
        urls.add(cabinCoreUrl);
        final FacadeCabinClasspathLauncher cabinClasspathLauncher =
                new FacadeCabinClasspathLauncher(mainClass, urls.toArray(new URL[0]));
        cabinClasspathLauncher.launch(new String[0]);
    }
}

class FacadeCabinJarLauncher extends CabinJarLauncher {

    FacadeCabinJarLauncher(final Archive archive) {
        super(archive);
    }

    @Override
    protected void invokeStart(Object container) {
        //DO nothing
        System.out.println("Facade Started");
    }
}

class FacadeCabinClasspathLauncher extends CabinClasspathLauncher {

    FacadeCabinClasspathLauncher(final String mainClass, final URL[] urls) {
        super(mainClass, urls);
    }

    @Override
    protected void invokeStart(Object container) {
        //DO nothing
        System.out.println("Facade Started");
    }
}

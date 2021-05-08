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

import io.esastack.cabin.api.domain.Module;
import io.esastack.cabin.api.service.share.SharedClassService;
import io.esastack.cabin.common.exception.CabinLoaderException;
import io.esastack.cabin.container.domain.LibModule;
import io.esastack.cabin.container.service.loader.LibModuleClassLoader;
import io.esastack.cabin.container.service.share.SharedClassServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class SharedClassServiceTest {

    @Test
    public void lazyLoadTest() {
        final String clazzName = "io.esastack.cabin.container.TestClass";
        final Module module = new LibModule.Builder().name("test").classLoader(new TestClassLoader()).build();
        SharedClassService sharedClassService = new SharedClassServiceImpl();
        sharedClassService.addSharedClass(clazzName, module);
        Assert.assertTrue(sharedClassService.containsClass(clazzName));
        Assert.assertTrue(sharedClassService.getSharedClass(clazzName).isAssignableFrom(TestClass.class));
        Assert.assertEquals(1, sharedClassService.getSharedClassCount());
        Assert.assertEquals(1, sharedClassService.getSharedClassMap().size());
        Assert.assertEquals(1, sharedClassService.getSharedClassMap().size());
        Assert.assertFalse(sharedClassService.getSharedClassMap().isEmpty());
        Assert.assertNotNull(sharedClassService.getSharedClassMap().get(clazzName));
        Assert.assertTrue(sharedClassService.getSharedClassMap().containsKey(clazzName));
    }
}

class TestClass {
}

class TestClassLoader extends LibModuleClassLoader {

    TestClassLoader() {
        super("test", new URL[0]);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws CabinLoaderException {
        return TestClass.class;
    }

    @Override
    public Class<?> loadClassFromClasspath(String name) throws CabinLoaderException {
        return TestClass.class;
    }
}

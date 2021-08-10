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
package io.esastack.cabin.container.service.loader;

import io.esastack.cabin.common.exception.CabinLoaderException;

import java.net.URL;

/**
 * Classes of unit test framework should be loaded by AppClassLoader:
 * 1. some of these classes is loaded by AppClassLoader before the CabinContainer is started, if then loaded by
 *    BizModuleClassLoader, some LinkErrs would happen;
 * 2. Cabin re-write a CabinSpringRunner, and loaded SpringRunner with UnitTestModuleClassLoader, its super class
 *    org.junit.runner.Runner should be loaded by AppClassLoader in order to be assigned correctly.
 */
public class UnitTestModuleClassLoader extends BizModuleClassLoader {

    private static final String[] PACKAGE_FOR_UNIT_TEST = {
            // Junit
            "org.junit", "junit", "org.hamcrest",
            // TestNG
            "org.testng", "com.beust.jcommander", "bsh",
            // tomcat
            "org.apache.catalina", "org.apache.coyote", "org.apache.juli", "org.apache.naming",
            "org.apache.tomcat", "org.apache.el", "javax"};

    public UnitTestModuleClassLoader(final URL[] urls) {
        super("UnitTestModule", urls);
    }

    @Override
    protected Class<?> loadClass0(final String name, final boolean resolve) throws CabinLoaderException {
        if (isUnitTestClass(name)) {
            try {
                return getSystemClassLoader().loadClass(name);
            } catch (Throwable ex) {
                throw new CabinLoaderException("Failed to load unit test class " + name, ex);
            }
        } else {
            return super.loadClass0(name, resolve);
        }
    }

    private boolean isUnitTestClass(final String name) {
        for (String pkg : PACKAGE_FOR_UNIT_TEST) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}

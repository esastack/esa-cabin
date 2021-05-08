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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class CabinModuleMojoTest {

    @Test
    public void getSpringbootFactoriesTest() {
        CabinModuleMojo moduleMojo = new CabinModuleMojo();
        URL resource = CabinModuleMojo.class.getClassLoader().getResource("META-INF/spring.factories");
        try {
            List<String> result = moduleMojo.getSpringbootSpiImpls(resource);
            Assert.assertEquals(2, result.size());
            Assert.assertEquals("io.esastack.servicekeeper.adapter.springboot.ServiceKeeperConfigurator",
                    result.get(0));
            Assert.assertEquals("io.esastack.servicekeeper.adapter.springboot.WebAutoSupportConfigurator",
                    result.get(1));
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }
}

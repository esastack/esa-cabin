package io.esastack.cabin.container.domain;

import org.junit.Assert;
import org.junit.Test;

public class JavaAgentModuleTest {

    @Test
    public void test() {
        JavaAgentModule javaAgentModule = JavaAgentModule.newBuilder()
                .classLoader(this.getClass().getClassLoader())
                .name("agent")
                .url(this.getClass().getClassLoader().getResource("cabin-core-0.1.0-SNAPSHOT.jar"))
                .build();
        Assert.assertNotNull(javaAgentModule.getClassLoader());
        Assert.assertNotNull(javaAgentModule.getName());
        Assert.assertNotNull(javaAgentModule.getUrl());
    }
}

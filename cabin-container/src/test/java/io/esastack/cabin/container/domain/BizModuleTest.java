package io.esastack.cabin.container.domain;

import io.esastack.cabin.container.domain.BizModule;
import org.junit.Assert;
import org.junit.Test;

public class BizModuleTest {

    @Test
    public void test() {

        BizModule bizModule = BizModule.newBuilder().build();
        Assert.assertNull(bizModule.getArguments());
        Assert.assertNull(bizModule.getClassLoader());
        Assert.assertNull(bizModule.getName());
        Assert.assertNull(bizModule.getMainClass());
        Assert.assertNull(bizModule.getMainMethod());
        Assert.assertNull(bizModule.getUrls());

        try {
            bizModule.start();
            Assert.fail();
        } catch (Throwable throwable) {
        }
    }
}

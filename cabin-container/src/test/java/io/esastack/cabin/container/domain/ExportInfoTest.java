package io.esastack.cabin.container.domain;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ExportInfoTest {

    @Test
    public void test() {
        ExportInfo exportInfo = new ExportInfo();

        exportInfo.addPackages(Arrays.asList("123"));
        exportInfo.addPackage("456");

        exportInfo.addJars(Arrays.asList("123"));
        exportInfo.addJar("456");

        exportInfo.addClasses(Arrays.asList("123.Test.class"));
        exportInfo.addClass("456.Test.class");

        Assert.assertNotNull(exportInfo.getPackages());
        Assert.assertNotNull(exportInfo.getJars());
        Assert.assertNotNull(exportInfo.getClasses());
    }
}

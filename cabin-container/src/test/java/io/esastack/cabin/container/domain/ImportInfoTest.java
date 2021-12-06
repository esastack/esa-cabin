package io.esastack.cabin.container.domain;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ImportInfoTest {

    @Test
    public void test() {
        ImportInfo importInfo = new ImportInfo();

        importInfo.addImportResources(Arrays.asList("123"));
        importInfo.addPackages(Arrays.asList("123"));
        importInfo.addPackage("456");

        importInfo.addClasses(Arrays.asList("123.Test.class"));
        importInfo.addClass("456.Test.class");

        Assert.assertNotNull(importInfo.getImportClassList());
        Assert.assertNotNull(importInfo.getImportResources());
        Assert.assertNotNull(importInfo.getImportPackageList());
    }
}

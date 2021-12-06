package io.esastack.cabin.loader.util;

import org.junit.Assert;
import org.junit.Test;

public class UrlUtilsTest {

    @Test
    public void test() {

        String url = UrlUtils.appendSegmentToPath(null, "123");
        Assert.assertEquals(url, "/123");
        url = UrlUtils.appendSegmentToPath("/", "123");
        Assert.assertEquals(url, "/123");
        url = UrlUtils.appendSegmentToPath("0/", "123");
        Assert.assertEquals(url, "0/123");
    }
}

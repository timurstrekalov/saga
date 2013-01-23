package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtilTest {

    @Mock
    HtmlPage htmlPage;

    @Test
    public void test_getFullSourcePath_absolute() throws Exception {
        final URL mockUrl = new URL("file:/D:/repos/git/ops/ops-ext4/target/jasmine/SpecRunner.html");

        when(htmlPage.getUrl()).thenReturn(mockUrl);

        final String sourceName = "file:/D:/repos/git/ops/ops-ext4/target/jasmine/src/app/utils/FieldLayout.js?_dc=1351820085178";
        final String fullSourcePath = Util.getFullSourcePath(htmlPage, sourceName);

        assertThat(fullSourcePath, equalTo("/D:/repos/git/ops/ops-ext4/target/jasmine/src/app/utils/FieldLayout.js"));
    }

    @Test
    public void test_getFullSourcePath_relative() throws Exception {
        final URL mockUrl = new URL("file:/D:/repos/git/ops/ops-ext4/target/jasmine/SpecRunner.html");

        when(htmlPage.getUrl()).thenReturn(mockUrl);

        final String sourceName = "src/app/utils/FieldLayout.js?_dc=1351820085178";
        final String fullSourcePath = Util.getFullSourcePath(htmlPage, sourceName);

        assertThat(fullSourcePath, equalTo("/D:/repos/git/ops/ops-ext4/target/jasmine/src/app/utils/FieldLayout.js"));
    }

}

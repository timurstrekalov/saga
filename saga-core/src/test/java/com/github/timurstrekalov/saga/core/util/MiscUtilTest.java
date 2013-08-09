package com.github.timurstrekalov.saga.core.util;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.timurstrekalov.saga.core.util.MiscUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MiscUtilTest {

    @Mock
    HtmlPage htmlPage;

    @Test
    public void test_getFullSourcePath_absolute() throws Exception {
        final URL mockUrl = new URL("file:/D:/repos/git/ops/ops-ext4/target/jasmine/SpecRunner.html");

        when(htmlPage.getUrl()).thenReturn(mockUrl);

        final String sourceName = "file:/D:/repos/git/ops/ops-ext4/target/jasmine/src/app/utils/FieldLayout.js?_dc=1351820085178";
        final String fullSourcePath = MiscUtil.getFullSourcePath(htmlPage, sourceName);
        if (File.separatorChar == '/') {
        	assertThat(fullSourcePath, equalTo("/D:/repos/git/ops/ops-ext4/target/jasmine/src/app/utils/FieldLayout.js"));
        } else {
        	assertThat(fullSourcePath, equalTo("D:\\repos\\git\\ops\\ops-ext4\\target\\jasmine\\src\\app\\utils\\FieldLayout.js"));
        }
    }

    @Test
    public void test_getFullSourcePath_relative() throws Exception {
        final URL mockUrl = new URL("file:/D:/repos/git/ops/ops-ext4/target/jasmine/SpecRunner.html");

        when(htmlPage.getUrl()).thenReturn(mockUrl);

        final String sourceName = "src/app/utils/FieldLayout.js?_dc=1351820085178";
        final String fullSourcePath = MiscUtil.getFullSourcePath(htmlPage, sourceName);

        if (File.separatorChar == '/') {
        	assertThat(fullSourcePath, equalTo("/D:/repos/git/ops/ops-ext4/target/jasmine/src/app/utils/FieldLayout.js"));
        } else {
        	assertThat(fullSourcePath, equalTo("D:\\repos\\git\\ops\\ops-ext4\\target\\jasmine\\src\\app\\utils\\FieldLayout.js"));
        }
    }

}

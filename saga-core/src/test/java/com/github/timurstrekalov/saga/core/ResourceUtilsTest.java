package com.github.timurstrekalov.saga.core;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ResourceUtilsTest {

    @Test
    public void testGetRelativePath() throws Exception {
        final String targetPath = "C:\\Users\\user\\Workspace\\dc\\dc\\target\\jasmine\\src\\src\\main\\javascript\\dc.js";
        final String basePath = "c:\\Users\\user\\Workspace\\dc";
        final String relativePath = ResourceUtils.getRelativePath(targetPath, basePath, "\\");

        assertThat(relativePath, equalTo("dc\\target\\jasmine\\src\\src\\main\\javascript\\dc.js"));
    }

}

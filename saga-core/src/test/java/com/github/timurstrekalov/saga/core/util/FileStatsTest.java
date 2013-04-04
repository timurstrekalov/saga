package com.github.timurstrekalov.saga.core.util;

import java.io.File;
import java.net.URI;

import com.github.timurstrekalov.saga.core.model.ScriptCoverageStatistics;
import com.github.timurstrekalov.saga.core.model.LineCoverageRecord;
import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileStatsTest {

    private static final String USER_DIR = System.getProperty("user.dir");

    @Test
    public void getParentName_file() {
        assertEquals("test/path/bla", new ScriptCoverageStatistics(new File(USER_DIR).toURI().toString(),new File(USER_DIR).toURI(), new File(USER_DIR + "/test/path/bla/file.js").toURI(),
                Lists.<LineCoverageRecord>newLinkedList(), true).getParentName());
    }

    @Test
    public void getParentName_http() {
        ScriptCoverageStatistics toTest = new ScriptCoverageStatistics(URI.create("http://localhost:8234").toString(),URI.create("http://localhost:8234"), URI.create("http://localhost:8234/test/path/bla/file.js"),
                Lists.<LineCoverageRecord>newLinkedList(), true);
        assertEquals("test/path/bla", toTest.getParentName());

        assertEquals("http://localhost:8234", toTest.getRelativePath());
    }

}

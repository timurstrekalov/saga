package com.github.timurstrekalov.saga.core.util;

import java.io.File;
import java.net.URI;

import com.github.timurstrekalov.saga.core.model.FileStats;
import com.github.timurstrekalov.saga.core.model.LineCoverageRecord;
import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileStatsTest {

    private static final String USER_DIR = System.getProperty("user.dir");

    @Test
    public void getParentName_file() {
        assertEquals("test/path/bla", new FileStats(new File(USER_DIR).toURI(), new File(USER_DIR + "/test/path/bla/file.js").toURI(),
                Lists.<LineCoverageRecord>newLinkedList(), true).getParentName());
    }

    @Test
    public void getParentName_http() {
        assertEquals("test/path/bla", new FileStats(URI.create("http://localhost:8234"), URI.create("http://localhost:8234/test/path/bla/file.js"),
                Lists.<LineCoverageRecord>newLinkedList(), true).getParentName());
    }

}

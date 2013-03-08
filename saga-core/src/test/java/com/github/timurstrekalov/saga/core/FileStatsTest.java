package com.github.timurstrekalov.saga.core;

import java.net.URI;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileStatsTest {

    private static final String USER_DIR = System.getProperty("user.dir");

    @Test
    public void getParentName_file() {
        assertEquals("test/path/bla", new FileStats(URI.create("file:" + USER_DIR), USER_DIR + "\\test\\path\\bla/file.js",
                Lists.<LineCoverageRecord>newLinkedList(), true).getParentName());
    }

    @Test
    public void getParentName_http() {
        assertEquals("test/path/bla", new FileStats(URI.create("http://localhost:8234"), "http://localhost:8234/test/path/bla/file.js",
                Lists.<LineCoverageRecord>newLinkedList(), true).getParentName());
    }

}

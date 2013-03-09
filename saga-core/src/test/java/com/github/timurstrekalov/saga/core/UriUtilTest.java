package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.net.URI;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import org.junit.Test;

import static com.github.timurstrekalov.saga.core.UriUtil.getLastSegment;
import static com.github.timurstrekalov.saga.core.UriUtil.getParent;
import static com.github.timurstrekalov.saga.core.UriUtil.isFileUri;
import static com.github.timurstrekalov.saga.core.UriUtil.toUri;
import static java.net.URI.create;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UriUtilTest {

    private static final String PWD = System.getProperty("user.dir");
    private static final String PWD_PARENT = new File(PWD).getParent();
    private static final URI PWD_PARENT_URI = new File(PWD_PARENT).toURI();

    private static final String HTTP = "http://localhost:8234";
    private static final URI HTTP_URI = create(HTTP);

    private static final String HTTP_WITH_PATH = "http://localhost:8234/some/file/here";
    private static final URI HTTP_WITH_PATH_URI = create(HTTP_WITH_PATH);

    private static final String HTTPS = "https://localhost:8234";
    private static final URI HTTPS_URI = create(HTTPS);

    private static final String FILE_ABS = Joiner.on(File.separatorChar).join("", "home", "user", "asd");
    private static final URI FILE_ABS_URI = toUri(FILE_ABS);

    private static final String FILE_REL = Joiner.on(File.separatorChar).join("..", "qweasd");
    private static final URI FILE_REL_URI = toUri(FILE_REL);

    @Test
    public void test_toUri_http() throws Exception {
        assertThat(HTTP_URI, equalTo(create(HTTP)));
    }

    @Test
    public void test_toUri_https() throws Exception {
        assertThat(HTTPS_URI, equalTo(create(HTTPS)));
    }

    @Test
    public void test_toUri_file_absolute() throws Exception {
        assertThat(FILE_ABS_URI, equalTo(create("file:" + FILE_ABS)));
    }

    @Test
    public void test_toUri_file_relative() throws Exception {
        assertThat(FILE_REL_URI, equalTo(create(PWD_PARENT_URI.toString() + "qweasd")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_toUri_unsupported_scheme() throws Exception {
        toUri("afp://someserver/file/path");
    }

    @Test
    public void test_isFileUri() throws Exception {
        assertThat(isFileUri(HTTP_URI), is(false));
        assertThat(isFileUri(HTTPS_URI), is(false));

        assertThat(isFileUri(FILE_ABS_URI), is(true));
        assertThat(isFileUri(FILE_REL_URI), is(true));
    }

    @Test
    public void test_getLastSegment() throws Exception {
        assertThat(getLastSegment(HTTP_URI), equalTo(Optional.<String>absent()));
        assertThat(getLastSegment(HTTP_WITH_PATH_URI), equalTo(Optional.of("here")));
        assertThat(getLastSegment(HTTPS_URI), equalTo(Optional.<String>absent()));

        assertThat(getLastSegment(FILE_ABS_URI), equalTo(Optional.of("asd")));
        assertThat(getLastSegment(FILE_REL_URI), equalTo(Optional.of("qweasd")));
    }

    @Test
    public void test_getParent() throws Exception {
        assertThat(getParent(HTTP_URI), equalTo("/"));
        assertThat(getParent(create("http://localhost:8080/")), equalTo("/"));

        assertThat(getParent(HTTP_WITH_PATH_URI), equalTo("/some/file"));
        assertThat(getParent(FILE_ABS_URI), equalTo("/home/user"));
        assertThat(getParent(FILE_REL_URI), equalTo(PWD_PARENT));
    }

}

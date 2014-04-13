package com.github.timurstrekalov.saga.core;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.github.timurstrekalov.saga.core.instrumentation.HtmlUnitBasedScriptInstrumenter;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

public class Data {

    private static final Map<String, String> data = Maps.newHashMap();
    private static final Properties props = new Properties();

    static {
        try {
            props.load(Data.class.getResourceAsStream("/project.properties"));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getClassJsSourceCode() {
        return lazyLoad("/tests/Class.js");
    }

    public static String getClassJsInstrumented(final String sourceName) {
        String body = lazyLoad("/pregen/Class.instrumented.js").replaceAll("<SOURCE_PLACEHOLDER>", sourceName);
        return HtmlUnitBasedScriptInstrumenter.COMPLETION_MONITOR + body;
    }

    private static String lazyLoad(final String name) {
        if (!data.containsKey(name)) {
            try {
                data.put(name, new String(ByteStreams.toByteArray(Data.class.getResourceAsStream(name))));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return data.get(name);
    }

    public static String getProperty(final String name) {
        return props.getProperty(name);
    }

}

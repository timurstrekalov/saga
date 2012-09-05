package com.github.timurstrekalov.saga.core;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.util.Map;

public class Data {

    private static final Map<String, String> data = Maps.newHashMap();

    public static String getClassJsSourceCode() {
        return lazyLoad("/tests/Class.js");
    }

    public static String getClassJsInstrumented() {
        return lazyLoad("/pregen/Class.instrumented.js");
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

}

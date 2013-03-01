package com.github.timurstrekalov.saga.core;

import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public enum Order {
    ASC,
    DESC;

    private static final Map<String, Order> map;

    public static Order fromString(final String name) {
        final String lowercaseName = name.toLowerCase(Locale.ENGLISH);

        if (!map.containsKey(lowercaseName)) {
            throw new IllegalArgumentException("Unknown Ordering name: " + name);
        }

        return map.get(lowercaseName);
    }

    static {
        final ImmutableMap.Builder<String, Order> builder = ImmutableMap.builder();

        builder.put("asc", ASC);
        builder.put("desc", DESC);

        builder.put("ascending", ASC);
        builder.put("descending", DESC);

        map = builder.build();
    }

}

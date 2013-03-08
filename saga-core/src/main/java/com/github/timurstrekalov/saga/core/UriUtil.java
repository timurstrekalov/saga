package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;

final class UriUtil {

    private static final Pattern supportedUriSchemeRe = Pattern.compile("(https?|file)");

    private UriUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    static URI toUri(final String s) {
        final URI uri = URI.create(s);

        if (uri.getScheme() != null) {
            final Matcher matcher = supportedUriSchemeRe.matcher(uri.getScheme());
            Preconditions.checkArgument(matcher.find(), "Supported URI schemes are: http, https and file");
            return uri;
        }

        return new File(s).toURI().normalize();
    }

    static boolean isFileUri(final URI baseDir) {
        return "file".equals(baseDir.getScheme());
    }

    static Optional<String> getLastSegment(final URI test) {
        final String path = test.getPath();

        if (StringUtils.isBlank(path)) {
            return Optional.absent();
        }

        if (!path.contains("/")) {
            return Optional.of(path);
        }

        return Optional.of(
                Iterables.getLast(
                        Splitter.on('/').
                                omitEmptyStrings().
                                trimResults().
                                split(path)
                )
        );
    }

}

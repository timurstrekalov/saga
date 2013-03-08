package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.codehaus.plexus.util.FileUtils;

class FileSystemTestFetcher implements TestFetcher {

    @Override
    public List<URI> fetch(final URI baseUri, final String includes, final String excludes) throws IOException {
        final File baseDir = new File(baseUri);
        Preconditions.checkState(baseDir.exists(), "baseDir doesn't exist");

        final List<File> tests = FileUtils.getFiles(baseDir, includes, excludes);

        return ImmutableList.copyOf(Lists.transform(tests, new Function<File, URI>() {
            @Override
            public URI apply(final File input) {
                return input.toURI();
            }
        }));
    }

}

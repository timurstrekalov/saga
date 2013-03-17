package com.github.timurstrekalov.saga.core.testfetcher;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface TestFetcher {

    List<URI> fetch(URI baseUri, String includes, String excludes) throws IOException;

}

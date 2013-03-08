package com.github.timurstrekalov.saga.core;

import java.io.IOException;
import java.net.URI;
import java.util.List;

interface TestFetcher {

    List<URI> fetch(URI baseUri, String includes, String excludes) throws IOException;

}

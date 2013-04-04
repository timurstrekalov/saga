package com.github.timurstrekalov.saga.core.cfg;

import java.io.File;
import java.net.URI;

/**
 * User: dervish
 * Date: 4/4/13
 * Time: 2:52 PM
 *
 * Basic implementation of grouped reporter related configuration parameters
 * propagated through the Maven mojo configuration or any other place.
 *
 */
public class ReporterConfig {
    /**
     * The URL of the base directory for the test search OR the web page with the tests.
     */
//    @Parameter(required = true)
    private URI baseUri;

    /**
     * The output directory for coverage reports.
     */
//    @Parameter(required = true)
    private File outputDir;
    /**
     * Name of raw LCOV report file with .dat extension.
     */
    private String rawName = Config.DEFAULT_RAW_NAME;
    /**
     * Parameter used to override base path of statistics files in reports.
     * Each source file in report will be prefixed with this base path. It might be useful
     * for thirdpaty software integration
     */
    private String relativePathBase;


    public String getRelativePathBase() {
        return relativePathBase;
    }

    public void setRelativePathBase(String relativePathBase) {
        this.relativePathBase = relativePathBase;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public String getRawName() {
        return rawName;
    }
}

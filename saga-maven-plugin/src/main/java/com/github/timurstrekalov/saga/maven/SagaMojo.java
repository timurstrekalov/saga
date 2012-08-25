package com.github.timurstrekalov.saga.maven;

import com.github.timurstrekalov.saga.core.CoverageGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

/**
 * @goal coverage
 * @phase verify
 * @threadSafe
 */
public class SagaMojo extends AbstractMojo {

    /**
     * @parameter
     * @required
     */
    private File baseDir;

    /**
     * @description A comma-separated list of Ant-style paths to tests to be run
     * @parameter
     * @required
     */
    private String includes;

    /**
     * @description A comma-separated list of Ant-style paths to exclude from the test run
     * @parameter
     */
    private String excludes;

    /**
     * @parameter
     * @required
     */
    private File outputDir;

    /**
     * @parameter
     */
    private Boolean outputInstrumentedFiles;

    /**
     * @parameter
     */
    private String[] noInstrumentPatterns;

    /**
     * @parameter
     */
    private Boolean cacheInstrumentedCode;

    /**
     * @parameter
     */
    private String outputStrategy;

    /**
     * @parameter
     */
    private Integer threadCount;

    /**
     * @parameter
     */
    private Boolean includeInlineScripts;

    /**
     * @parameter
     */
    private Long backgroundJavaScriptTimeout;

    /**
     * @parameter
     */
    private String sourcesToPreload;

    public void execute() throws MojoExecutionException {
        try {

            final CoverageGenerator gen = new CoverageGenerator(baseDir, includes, excludes, outputDir);

            gen.setOutputInstrumentedFiles(outputInstrumentedFiles);
            gen.setCacheInstrumentedCode(cacheInstrumentedCode);
            gen.setNoInstrumentPatterns(noInstrumentPatterns);
            gen.setOutputStrategy(outputStrategy);
            gen.setThreadCount(threadCount);
            gen.setIncludeInlineScripts(includeInlineScripts);
            gen.setBackgroundJavaScriptTimeout(backgroundJavaScriptTimeout);
            gen.setSourcesToPreload(sourcesToPreload);

            try {
                gen.run();
            } catch (final IOException e) {
                throw new MojoExecutionException("Error generating coverage", e);
            }
        } catch (final IllegalArgumentException e) {
            throw new MojoExecutionException("Caught IllegalArgumentException: illegal POM parameters?", e);
        }
    }

}

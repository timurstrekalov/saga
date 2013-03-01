package com.github.timurstrekalov.saga.maven;

import java.io.File;
import java.io.IOException;

import com.github.timurstrekalov.saga.core.CoverageGenerator;
import com.github.timurstrekalov.saga.core.CoverageGenerators;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "coverage", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SagaMojo extends AbstractMojo {

    /**
     * The base directory for the test search.
     */
    @Parameter(required = true)
    private File baseDir;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to include in the search for test runners.
     */
    @Parameter(required = true)
    private String includes;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to exclude from the search for test runners.
     */
    @Parameter
    private String excludes;

    /**
     * The output directory for coverage reports.
     */
    @Parameter(required = true)
    private File outputDir;

    /**
     * Whether to output instrumented files. Will be written to ${outputDir}/instrumented.
     */
    @Parameter(defaultValue = "false")
    private Boolean outputInstrumentedFiles;

    /**
     * A list of regular expressions to match source file paths to be excluded from instrumentation.
     */
    @Parameter
    private String[] noInstrumentPatterns;

    /**
     * Whether to cache instrumented source code. It's entirely possible that two tests might load some of the same
     * resources - this would prevent them from being instrumented every time, but rather cache them for the whole
     * coverage run.
     */
    @Parameter(defaultValue = "true")
    private Boolean cacheInstrumentedCode;

    /**
     * One of TOTAL, PER_TEST or BOTH. Pretty self-explanatory.
     */
    @Parameter(defaultValue = "TOTAL")
    private String outputStrategy;

    /**
     * The maximum number of threads to use.
     */
    @Parameter
    private Integer threadCount;

    /**
     * Whether to include inline scripts into instrumentation.
     */
    @Parameter(defaultValue = "false")
    private Boolean includeInlineScripts;

    /**
     * How long to wait for background JS jobs to finish (in milliseconds).
     */
    @Parameter(defaultValue = "300000")
    private Long backgroundJavaScriptTimeout;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to exclude from the search for sources to preload (useful to generate total coverage even though your tests might
     * not reference certain files, especially when you simply don't have tests for classes but you DO want to see them).
     * Paths are expected to be provided relative to the base directory.
     */
    @Parameter
    private String sourcesToPreload;

    /**
     * Encoding to use when preloading sources.
     */
    @Parameter(defaultValue = "UTF-8")
    private String sourcesToPreloadEncoding;

    /**
     * Determines the browser and version profile that HtmlUnit will simulate. This maps 1-to-1 with the public static
     * instances found in <a href="http://htmlunit.sourceforge.net/apidocs/com/gargoylesoftware/htmlunit/BrowserVersion.html">com.gargoylesoftware.htmlunit.BrowserVersion</a>.
     * <br/><br/>
     *
     * Some valid examples:
     * <ul>
     *     <li>FIREFOX_3_6</li>
     *     <li>FIREFOX_10</li>
     *     <li>INTERNET_EXPLORER_6</li>
     *     <li>INTERNET_EXPLORER_7</li>
     *     <li>INTERNET_EXPLORER_8</li>
     *     <li>CHROME_16</li>
     * </ul>
     */
    @Parameter(defaultValue = "FIREFOX_3_6")
    private String browserVersion;

    /**
     * A comma-separated list of formats of the reports to be generated. Valid values are:
     * <ul>
     *     <li>HTML</li>
     *     <li>RAW</li>
     *     <li>CSV</li>
     *     <li>PDF</li>
     * </ul>
     */
    @Parameter(defaultValue = "HTML, RAW")
    private String reportFormats;

    /**
     * The column to sort by, one of 'file', 'statements', 'executed' or 'coverage'.
     */
    @Parameter(defaultValue = "coverage")
    private String sortBy;

    /**
     * The order of sorting, one of 'asc' or 'ascending', 'desc' or 'descending'.
     */
    @Parameter(defaultValue = "ascending")
    private String order;
    
    @Override
    public void execute() throws MojoExecutionException {
        try {

            final CoverageGenerator gen = CoverageGenerators.newInstance(baseDir, includes, excludes, outputDir);

            gen.setOutputInstrumentedFiles(outputInstrumentedFiles);
            gen.setCacheInstrumentedCode(cacheInstrumentedCode);
            gen.setNoInstrumentPatterns(noInstrumentPatterns);
            gen.setOutputStrategy(outputStrategy);
            gen.setThreadCount(threadCount);
            gen.setIncludeInlineScripts(includeInlineScripts);
            gen.setBackgroundJavaScriptTimeout(backgroundJavaScriptTimeout);
            gen.setSourcesToPreload(sourcesToPreload);
            gen.setSourcesToPreloadEncoding(sourcesToPreloadEncoding);
            gen.setBrowserVersion(browserVersion);
            gen.setReportFormats(reportFormats);
            gen.setSortBy(sortBy);
            gen.setOrder(order);

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

package com.github.timurstrekalov.saga.maven;

import java.io.File;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.CoverageGenerator;
import com.github.timurstrekalov.saga.core.CoverageGeneratorFactory;
import com.github.timurstrekalov.saga.core.cfg.ReporterConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "coverage", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SagaMojo extends AbstractMojo {

    /**
     * Basic parameters to customize reporting features of plugin.
     * encapsulates baseUri, outputDir, rawName, relativePathBase
     */
    @Parameter
    private ReporterConfig reporterConfig;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to include in the search for test runners.<br/><br/>
     *
     * Note that this parameter only makes sense if {@link #reporterConfig.baseUri} is a filesystem URL.
     */
    @Parameter
    private String includes;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to exclude from the search for test runners.<br/><br/>
     *
     * Note that this parameter only makes sense if {@link #reporterConfig.baseUri} is a filesystem URL.
     */
    @Parameter
    private String excludes;

    /**
     * Whether to output instrumented files. Will be written to ${outputDir}/instrumented.
     */
    @Parameter(defaultValue = "false")
    private Boolean outputInstrumentedFiles;

    /**
     * A list of regular expressions to match source file URLs to be excluded from instrumentation.
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
     * Paths are expected to be provided relative to the base directory.<br/><br/>
     *
     * Note that this parameter only makes sense if {@link #reporterConfig.baseUri} is a filesystem URL.
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
     *     <li>FIREFOX_17</li>
     *     <li>CHROME</li>
     *     <li>INTERNET_EXPLORER_7</li>
     *     <li>INTERNET_EXPLORER_8</li>
     *     <li>INTERNET_EXPLORER_9</li>
     * </ul>
     */
    @Parameter(defaultValue = "FIREFOX_17")
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

    /**
     * Enables skipping the coverage reporting.
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;



    @Override
    public void execute() throws MojoExecutionException {
        if (skipTests) {
            getLog().info("Coverage reporting is skipped.");
            return;
        }

        try {
            final CoverageGenerator gen = CoverageGeneratorFactory.newInstance(reporterConfig.getBaseUri().toString(), reporterConfig.getOutputDir());
            final Config config = gen.getConfig();
            config.setIncludes(includes);
            config.setExcludes(excludes);
            config.setOutputInstrumentedFiles(outputInstrumentedFiles);
            config.setCacheInstrumentedCode(cacheInstrumentedCode);
            config.setNoInstrumentPatterns(noInstrumentPatterns);
            config.setThreadCount(threadCount);
            config.setIncludeInlineScripts(includeInlineScripts);
            config.setBackgroundJavaScriptTimeout(backgroundJavaScriptTimeout);
            config.setSourcesToPreload(sourcesToPreload);
            config.setSourcesToPreloadEncoding(sourcesToPreloadEncoding);
            config.setBrowserVersion(browserVersion);
            config.setReportFormats(reportFormats);
            config.setSortBy(sortBy);
            config.setOrder(order);
            config.setReporterConfig(reporterConfig);

            gen.instrumentAndGenerateReports();
        } catch (final IllegalArgumentException e) {
            throw new MojoExecutionException("Caught IllegalArgumentException: illegal parameters?", e);
        } catch (final Exception e) {
            throw new MojoExecutionException("Error generating coverage", e);
        }
    }


    public ReporterConfig getReporterConfig() {
        return this.reporterConfig;
    }

}

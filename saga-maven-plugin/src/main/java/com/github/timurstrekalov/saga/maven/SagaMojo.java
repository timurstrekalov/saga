package com.github.timurstrekalov.saga.maven;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.github.timurstrekalov.saga.core.CoverageGenerator;
import com.github.timurstrekalov.saga.core.CoverageGeneratorFactory;
import com.github.timurstrekalov.saga.core.cfg.Config;
import com.google.common.collect.Lists;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "coverage", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SagaMojo extends AbstractMojo {

    /**
     * The URL of the base directory for the test search OR the web page with the tests.
     */
    @Parameter(required = true)
    private String baseDir;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to include in the search for test runners.<br/><br/>
     *
     * Note that this parameter only makes sense if {@link #baseDir} is a filesystem URL.
     */
    @Parameter
    private String includes;

    /**
     * A comma-separated list of <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant-style patterns</a>
     * to exclude from the search for test runners.<br/><br/>
     *
     * Note that this parameter only makes sense if {@link #baseDir} is a filesystem URL.
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
     * Paths are expected to be provided relative to the base directory.<br/><br/>
     *
     * Note that this parameter only makes sense if {@link #baseDir} is a filesystem URL.
     */
    @Parameter
    private String sourcesToPreload;

    /**
     * Encoding to use when preloading sources.
     */
    @Parameter(defaultValue = Config.DEFAULT_SOURCES_TO_PRELOAD_ENCODING)
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
     *     <li>COBERTURA</li>
     * </ul>
     */
    @Parameter(defaultValue = "HTML, RAW, COBERTURA")
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
    
    /**
     * Below this percentage, test will not pass
     */
    @Parameter(property = "minCoveragePercentage", defaultValue = "0")
    private int minCoveragePercentage;

    /**
     * The {@link org.openqa.selenium.WebDriver} implementation to use - you can find the available implementations on their
     * <a href="http://docs.seleniumhq.org/projects/webdriver/">website</a>. Note that if using any but the HtmlUnitDriver or
     * PhantomJSDriver, you have to make sure the appropriate driver is on the classpath. Valid examples include:
     * <ul>
     *     <li>org.openqa.selenium.htmlunit.HtmlUnitDriver</li>
     *     <li>org.openqa.selenium.phantomjs.PhantomJSDriver</li>
     *     <li>org.openqa.selenium.firefox.FirefoxDriver</li>
     *     <li>... and many others</li>
     * </ul>
     */
    @Parameter(defaultValue = Config.DEFAULT_WEB_DRIVER_CLASS_NAME)
    private String webDriverClassName;

    /**
     * Specify a map of desired capabilities to be passed to the {@link org.openqa.selenium.WebDriver} instance. When using PhantomJS,
     * specify the "phantomjs.binary.path" property containing the path to the "phantomjs" binary if it's not on the system PATH.
     */
    @Parameter
    private Map<String, String> webDriverCapabilities;

    /**
     * Absolute path to the sources.  This is used when generating COBERTURA style reports.
     */
    @Parameter
    private List<String> sourceDirs;

    /**
     * This parameter must be used instead of using a defaultValue on the "sourceDirs" parameter in
     * order to overcome http://jira.codehaus.org/browse/MNG-5440
     */
    @Parameter(defaultValue = "${basedir}", readonly = true)
    private String defaultSourceDir;

    @Override
    public void execute() throws MojoExecutionException {
        if (skipTests) {
            getLog().info("Coverage reporting is skipped.");
            return;
        }

        if(System.getProperty("minCoveragePercentage") != null) {
            minCoveragePercentage = Integer.parseInt(System.getProperty("minCoveragePercentage"));
        }
        
        try {
            final CoverageGenerator gen = CoverageGeneratorFactory.newInstance(baseDir, outputDir);
            final Config config = gen.getConfig();

            config.setIncludes(includes);
            config.setExcludes(excludes);
            config.setOutputInstrumentedFiles(outputInstrumentedFiles);
            config.setCacheInstrumentedCode(cacheInstrumentedCode);
            config.setNoInstrumentPatterns(noInstrumentPatterns);
            config.setOutputStrategy(outputStrategy);
            config.setThreadCount(threadCount);
            config.setIncludeInlineScripts(includeInlineScripts);
            config.setBackgroundJavaScriptTimeout(backgroundJavaScriptTimeout);
            config.setSourcesToPreload(sourcesToPreload);
            config.setSourcesToPreloadEncoding(sourcesToPreloadEncoding);
            config.setBrowserVersion(browserVersion);
            config.setReportFormats(reportFormats);
            config.setSortBy(sortBy);
            config.setOrder(order);
            config.setWebDriverCapabilities(webDriverCapabilities);
            config.setWebDriverClassName(webDriverClassName);
            config.setMinCoveragePercentage(minCoveragePercentage);

            if (sourceDirs == null) {
              sourceDirs = Lists.newArrayList(defaultSourceDir);
            } else if (sourceDirs.isEmpty()) {
              sourceDirs.add(defaultSourceDir);
            }
            config.setSourceDir(sourceDirs);

            gen.instrumentAndGenerateReports();
        } catch (final IllegalArgumentException e) {
            throw new MojoExecutionException("Caught IllegalArgumentException: illegal parameters?", e);
        } catch (final Exception e) {
            throw new MojoExecutionException("Error generating coverage", e);
        }
    }

}

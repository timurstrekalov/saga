package com.github.timurstrekalov;

import com.google.common.collect.ImmutableList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @goal coverage
 * @phase verify
 * @threadSafe
 */
public class CoverageMojo extends AbstractMojo {

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
    private OutputStrategy outputStrategy;

    public void execute() throws MojoExecutionException {
        final CoverageGenerator gen = new CoverageGenerator();

        final File[] files;

        try {
            final List list = FileUtils.getFiles(baseDir, includes, excludes);
            files = new File[list.size()];

            for (int i = 0; i < list.size(); i++) {
                files[i] = (File) list.get(i);
            }
        } catch (final IOException e) {
            throw new MojoExecutionException("Error looking up tests to run", e);
        }

        gen.setTests(files);
        gen.setOutputDir(outputDir);

        if (outputInstrumentedFiles != null) {
            gen.setOutputInstrumentedFiles(outputInstrumentedFiles);
        }

        if (cacheInstrumentedCode != null) {
            gen.setCacheInstrumentedCode(cacheInstrumentedCode);
        }

        if (noInstrumentPatterns != null) {
            gen.setNoInstrumentPatterns(ImmutableList.copyOf(noInstrumentPatterns));
        }

        if (outputStrategy != null) {
            gen.setOutputStrategy(outputStrategy);
        }

        try {
            gen.run();
        } catch (final IOException e) {
            throw new MojoExecutionException("Error generating coverage", e);
        }
    }

}

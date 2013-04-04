package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import com.google.common.reflect.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverageGeneratorFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoverageGenerator.class);

    private CoverageGeneratorFactory() {
        throw new UnsupportedOperationException("Factory class");
    }

    public static CoverageGenerator newInstance(final String baseDir, final File outputDir) {
        final CoverageGenerator delegate = new DefaultCoverageGenerator();
        delegate.getConfig().getReporterConfig().setBaseUri(UriUtil.toUri(baseDir));
        delegate.getConfig().getReporterConfig().setOutputDir(outputDir);

        return Reflection.newProxy(CoverageGenerator.class, new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().startsWith("set")) {
                    logger.debug("{}({})", method.getName(), Arrays.toString(args));
                }

                return method.invoke(delegate, args);
            }
        });
    }

    public static CoverageGenerator newInstance(final Config config) {
        final CoverageGenerator delegate = new DefaultCoverageGenerator(config);

        return Reflection.newProxy(CoverageGenerator.class, new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().startsWith("set")) {
                    logger.debug("{}({})", method.getName(), Arrays.toString(args));
                }

                return method.invoke(delegate, args);
            }
        });
    }

}

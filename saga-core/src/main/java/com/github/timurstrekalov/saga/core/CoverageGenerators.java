package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.google.common.reflect.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverageGenerators {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoverageGenerator.class);

    public static CoverageGenerator newInstance(final File baseDir, final String includes, final String excludes, final File outputDir) {
        final CoverageGenerator delegate = new DefaultCoverageGenerator(baseDir, includes, excludes, outputDir);

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

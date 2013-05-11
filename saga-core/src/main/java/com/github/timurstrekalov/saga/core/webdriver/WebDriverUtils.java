package com.github.timurstrekalov.saga.core.webdriver;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import org.openqa.selenium.JavascriptExecutor;

public final class WebDriverUtils {

    private static final int DEFAULT_TIMEOUT_IN_SECONDS = 15;

    private WebDriverUtils() {
        throw new UnsupportedOperationException("Non-instantiable");
    }

    public static void waitForWindowJavaScriptVariableToBePresent(final JavascriptExecutor executor, final String variableName) {
        new SafeJavascriptWait(executor)
                .withTimeout(DEFAULT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(new Predicate<JavascriptExecutor>() {

                    @Override
                    public boolean apply(final JavascriptExecutor input) {
                        return (Boolean) executor.executeScript("return !!window." + variableName);
                    }
                });
    }

}

package com.github.timurstrekalov.saga.core.webdriver;

import com.google.common.base.Predicate;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does not throw an exception if a timeout is reached.
 */
public final class SafeJavascriptWait extends FluentWait<JavascriptExecutor> {

    private static final Logger logger = LoggerFactory.getLogger(SafeJavascriptWait.class);

    public SafeJavascriptWait(final JavascriptExecutor input) {
        super(input);
    }

    @Override
    public void until(final Predicate<JavascriptExecutor> isTrue) {
        try {
            super.until(isTrue);
        } catch (final TimeoutException e) {
            logger.debug(e.getMessage());
        }
    }

}
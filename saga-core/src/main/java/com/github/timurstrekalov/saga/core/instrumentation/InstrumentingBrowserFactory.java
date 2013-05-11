package com.github.timurstrekalov.saga.core.instrumentation;

import com.github.timurstrekalov.saga.core.cfg.Config;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public final class InstrumentingBrowserFactory {

    private InstrumentingBrowserFactory() {
        throw new UnsupportedOperationException("Non-instantiable");
    }

    public static InstrumentingBrowser newInstance(final Config config) {
        final String webDriverClassName = config.getWebDriverClassName();

        if (HtmlUnitDriver.class.getName().equals(webDriverClassName)) {
            return new HtmlUnitInstrumentingBrowser(config);
        }

        return new GenericInstrumentingBrowser(config);
    }

}

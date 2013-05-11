package com.github.timurstrekalov.saga.core.server;

import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentingProxyServer {

    private final Server server;
    private final ScriptInstrumenter instrumenter;

    public InstrumentingProxyServer(final ScriptInstrumenter instrumenter) {
        this.instrumenter = instrumenter;

        server = new Server();
    }

    private static final Logger logger = LoggerFactory.getLogger(InstrumentingProxyServer.class);

    public int start() {
        final SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(0);

        server.addConnector(connector);

        final HandlerCollection handlers = new HandlerCollection();

        // Setup proxy servlet
        final ServletContextHandler context = new ServletContextHandler(handlers, "/");
        context.setAttribute(InstrumentingProxyServlet.INSTRUMENTER, instrumenter);
        context.addServlet(new ServletHolder(InstrumentingProxyServlet.class), "/*");

        // Setup proxy handler to handle CONNECT methods
        final ConnectHandler connectProxy = new ConnectHandler();
        handlers.addHandler(connectProxy);

        server.setHandler(handlers);

        try {
            server.start();
            logger.info("Proxy server started on port {}", connector.getLocalPort());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return connector.getLocalPort();
    }

    public void stop() {
        try {
            server.stop();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}

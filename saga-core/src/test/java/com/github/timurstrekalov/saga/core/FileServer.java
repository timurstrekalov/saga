package com.github.timurstrekalov.saga.core;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileServer {

    private final Server server;
    private final String resourceBase;

    public FileServer(final String resourceBase) {
        this.resourceBase = resourceBase;
        server = new Server();
    }

    private static final Logger logger = LoggerFactory.getLogger(FileServer.class);

    public int start() {
        final SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(0);

        server.addConnector(connector);

        final HandlerCollection handlers = new HandlerCollection();
        final ResourceHandler resourceHandler = new ResourceHandler() {
            @Override
            protected void doResponseHeaders(final HttpServletResponse response, final Resource resource, final String mimeType) {
                response.addDateHeader("Expires", 0L);
            }
        };

        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(resourceBase);

        handlers.addHandler(resourceHandler);

        server.setHandler(handlers);

        try {
            server.start();
            logger.info("File server started on port {}", connector.getLocalPort());
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

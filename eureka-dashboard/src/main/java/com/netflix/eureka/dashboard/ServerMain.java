package com.netflix.eureka.dashboard;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ServerMain {

    public void start() throws Exception {

        initPlatform();


        final Server server = new Server(8080);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase(".");

        ServletContextHandler svrContextHandler = new ServletContextHandler();
        svrContextHandler.addServlet(new ServletHolder(new SystemViewWebSocketServlet()), "/websocket");
        svrContextHandler.addServlet(new ServletHolder(new HealthCheckServlet()), "/healthcheck");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, svrContextHandler, new DefaultHandler()});
        server.setHandler(handlers);

        server.start();
        server.join();
    }

    private void initPlatform() {
        System.setProperty("netflix.environment", "test");
        System.setProperty("archaius.configurationSource.defaultFileName", "application.properties");

        Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(AtlasClientFacade.class).to(AtlasClientFacadeImpl.class);

            }
        });
    }



    public static void main(String[] args) throws Exception {
        ServerMain serverMain = new ServerMain();
        serverMain.start();
    }

}

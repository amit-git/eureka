package com.netflix.eureka.dashboard;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;


public class SystemViewWebSocketServlet extends WebSocketServlet {
    @Override
    public void configure(WebSocketServletFactory factory) {
        // register a socket class as default
        factory.register(SystemViewWebSocket.class);

    }
}

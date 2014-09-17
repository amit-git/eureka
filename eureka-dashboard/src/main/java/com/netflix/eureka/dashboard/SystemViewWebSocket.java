package com.netflix.eureka.dashboard;


import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;


public class SystemViewWebSocket extends WebSocketAdapter {

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        //getSession().setIdleTimeout(300000);
        System.out.println("Default idle timeout " + getSession().getIdleTimeout());
    }

    @Override
    public void onWebSocketText(final String message) {
        if (isNotConnected()) {
            return;
        }

        try {
            getRemote().sendString("Got " + message.toUpperCase());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

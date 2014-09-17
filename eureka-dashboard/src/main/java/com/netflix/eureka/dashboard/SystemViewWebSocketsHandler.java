package com.netflix.eureka.dashboard;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.servo.ServoEventsListenerFactory;
import rx.Observable;
import rx.functions.Func1;

public class SystemViewWebSocketsHandler {

    static final int DEFAULT_PORT = 8090;

    private final int port;

    static {
        RxNetty.useMetricListenersFactory(new ServoEventsListenerFactory("rx-netty-examples-client",
                "rx-netty-examples-server"));
    }

    public SystemViewWebSocketsHandler(int port) {
        this.port = port;
    }

    public RxServer<WebSocketFrame, WebSocketFrame> createServer() {
        RxServer<WebSocketFrame, WebSocketFrame> server = RxNetty.newWebSocketServerBuilder(port, new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
            @Override
            public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
                return connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(WebSocketFrame wsFrame) {
                        TextWebSocketFrame textFrame = (TextWebSocketFrame) wsFrame;
                        System.out.println("Got message: " + textFrame.text());
                        return connection.writeAndFlush(new TextWebSocketFrame(textFrame.text().toUpperCase()));
                    }
                });
            }
        }).withWebSocketURI("/system-view").enableWireLogging(LogLevel.ERROR).build();

        System.out.println("WebSocket server started...");
        return server;
    }

    public static void main(final String[] args) {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        new SystemViewWebSocketsHandler(DEFAULT_PORT).createServer().startAndWait();
    }
}

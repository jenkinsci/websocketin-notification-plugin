package io.jenkins.plugins;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class WebsocketInClient extends WebSocketClient {
    private final String startMessage;

    private Exception exception;

    public WebsocketInClient(URI serverUri, String startMessage, Map<String, String> additionalHttpHeaders) {
        super(serverUri, additionalHttpHeaders);
        this.startMessage = startMessage;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        if(startMessage != null && !startMessage.equals("")) {
            this.send(startMessage);
        }
    }

    @Override
    public void onMessage(String s) {
    }

    @Override
    public void onClose(int i, String s, boolean b) {
    }

    @Override
    public void onError(Exception e) {
        exception = e;
    }

    public Exception getException() {
        return exception;
    }
}

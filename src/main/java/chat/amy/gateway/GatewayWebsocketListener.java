package chat.amy.gateway;

import chat.amy.AmybotShard;
import chat.amy.AmybotShard.InternalEvent;
import chat.amy.gateway.GatewayConnection.State;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.IOException;

/**
 * @author amy
 * @since 9/4/17.
 */
final class GatewayWebsocketListener extends WebSocketListener {
    private final AmybotShard shard;
    private final GatewayConnection gatewayConnection;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HeartbeatService heartbeatService;
    
    @Getter
    private boolean finishedConnecting;
    
    GatewayWebsocketListener(final AmybotShard shard, final GatewayConnection gatewayConnection) {
        this.shard = shard;
        this.gatewayConnection = gatewayConnection;
        heartbeatService = new HeartbeatService(gatewayConnection);
    }
    
    @Override
    public void onOpen(final WebSocket webSocket, final Response response) {
        gatewayConnection.getSocketState().set(State.CONNECTED);
        shard.getLogger().info("Connected to the gateway!");
        try {
            webSocket.send(mapper.writeValueAsString(new GatewayMessage(0, null)));
        } catch(final JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onMessage(final WebSocket webSocket, final String text) {
        try {
            final GatewayMessage payload = mapper.readValue(text, GatewayMessage.class);
            if(payload.getOp() != 1) {
                shard.getLogger().info("Got a message from the gateway: '" + text + "'.");
            }
            switch(payload.getOp()) {
                case 0:
                    if(!finishedConnecting) {
                        shard.getLogger().info("Starting up heartbeats...");
                        heartbeatService.startAsync();
                        finishedConnecting = true;
                        shard.getEventBus().post(InternalEvent.READY);
                    }
                    break;
                case 1:
                    // TODO: Do we care?
                    break;
            }
        } catch(final IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onClosing(final WebSocket webSocket, final int code, final String reason) {
        gatewayConnection.getSocketState().set(State.DISCONNECTED);
        shard.getEventBus().post(InternalEvent.DISCONNECT);
        shard.getLogger().info("Gateway connection closing with code " + code + ": " + reason);
        heartbeatService.stopAsync();
        shard.getGatewayConnection().reconnect(code, reason);
    }
    
    @Override
    public void onClosed(final WebSocket webSocket, final int code, final String reason) {
        gatewayConnection.getSocketState().set(State.DISCONNECTED);
        shard.getLogger().info("Gateway connection closed with code " + code + ": " + reason);
    }
    
    @Override
    public void onFailure(final WebSocket webSocket, final Throwable t, final Response response) {
        gatewayConnection.getSocketState().set(State.DISCONNECTED);
        shard.getEventBus().post(InternalEvent.DISCONNECT);
        shard.getLogger().info("Gateway connection failure:", t);
        shard.getGatewayConnection().reconnect(1011, "Failure: " + t.getClass().getName() + ": " + t.getMessage());
    }
}

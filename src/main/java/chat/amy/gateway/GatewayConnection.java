package chat.amy.gateway;

import chat.amy.AmybotShard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.Getter;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author amy
 * @since 9/4/17.
 */
@SuppressWarnings({"WeakerAccess", "UnnecessarilyQualifiedInnerClassAccess"})
public class GatewayConnection {
    @Getter
    private final AmybotShard shard;
    private final ObjectMapper mapper = new ObjectMapper();
    private final BlockingQueue<GatewayMessage> queue = new LinkedBlockingDeque<>();
    @Getter
    private final AtomicReference<State> socketState = new AtomicReference<>(State.DISCONNECTED);
    @Getter
    private WebSocket websocket;
    private final MessagePoller messagePoller = new MessagePoller();
    
    private final class MessagePoller extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            if(socketState.get() == GatewayConnection.State.CONNECTED) {
                if(!queue.isEmpty()) {
                    try {
                        final GatewayMessage message = queue.take();
                        websocket.send(mapper.writeValueAsString(message));
                    } catch(final InterruptedException | JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    
        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0L, 50L, TimeUnit.MILLISECONDS);
        }
    }
    
    public GatewayConnection(final AmybotShard shard) {
        this.shard = shard;
    }
    
    public void queue(final GatewayMessage message) {
        queue.add(message);
    }
    
    public void connect() {
        messagePoller.startAsync();
        socketState.set(State.CONNECTING);
        shard.getLogger().info("Connecting to the gateway...");
        websocket = shard.getClient().newWebSocket(new Request.Builder().url("ws://gateway:8080").build(), new GatewayWebsocketListener(shard, this));
    }
    
    public void reconnect(final int code, final String reason) {
        messagePoller.stopAsync();
        shard.getLogger().info("Finishing closing connection...");
        websocket.close(code, reason);
        if(!queue.isEmpty()) {
            while(queue.peek().getOp() == 1) {
                queue.remove();
            }
        }
        socketState.set(State.RECONNECTING);
        try {
            Thread.sleep(2000L);
        } catch(final InterruptedException e) {
            e.printStackTrace();
        }
        connect();
    }
    
    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
    }
}

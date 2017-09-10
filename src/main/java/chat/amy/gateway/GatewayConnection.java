package chat.amy.gateway;

import chat.amy.AmybotShard;
import chat.amy.jda.WrappedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.Getter;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author amy
 * @since 9/4/17.
 */
@SuppressWarnings("WeakerAccess")
public final class GatewayConnection {
    // This being static shouldn't matter, because you should only ever have one instance of this class anyway
    private static final BlockingQueue<GatewayMessage> queue = new LinkedBlockingDeque<>();
    @Getter
    private final AmybotShard shard;
    private final ObjectMapper mapper = new ObjectMapper();
    @Getter
    private final AtomicReference<State> socketState = new AtomicReference<>(State.DISCONNECTED);
    @SuppressWarnings("TypeMayBeWeakened")
    private final MessagePoller messagePoller = new MessagePoller();
    @Getter
    private WebSocket websocket;
    
    public GatewayConnection(final AmybotShard shard) {
        this.shard = shard;
    }
    
    public static void externalQueue(final WrappedEvent input) {
        queue(new GatewayMessage(2, input));
    }
    
    public static void queue(final GatewayMessage message) {
        queue.add(message);
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public void connect() {
        if(!messagePoller.isRunning()) {
            messagePoller.startAsync();
        }
        socketState.set(State.CONNECTING);
        shard.getLogger().info("Connecting to the gateway...");
        websocket = shard.getClient().newWebSocket(new Request.Builder().url(Optional.ofNullable(System.getenv("GATEWAY_URL"))
                        .orElse("ws://gateway:8080")).build(),
                new GatewayWebsocketListener(shard, this));
    }
    
    public void reconnect(final int code, final String reason) {
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
    
    private final class MessagePoller extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            if(socketState.get() == GatewayConnection.State.CONNECTED && !queue.isEmpty()) {
                try {
                    final GatewayMessage message = queue.take();
                    websocket.send(mapper.writeValueAsString(message));
                } catch(final InterruptedException | JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0L, 5L, TimeUnit.MILLISECONDS);
        }
    }
}

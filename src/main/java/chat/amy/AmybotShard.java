package chat.amy;

import chat.amy.jda.WrappedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author amy
 * @since 9/2/17.
 */
@SuppressWarnings({"unused", "UnnecessarilyQualifiedInnerClassAccess", "WeakerAccess", "FieldCanBeLocal"})
public final class AmybotShard {
    @Getter
    private static final EventBus eventBus = new EventBus();
    private static final String RABBITMQ_QUEUE_FORMAT = "discord-shard-%s-%s";
    private static final String GATEWAY_QUEUE = Optional.of(System.getenv("GATEWAY_QUEUE")).orElse("gateway");
    @Getter
    @SuppressWarnings("TypeMayBeWeakened")
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build();
    @Getter
    private final Logger logger = LoggerFactory.getLogger("amybot-shard");
    @Getter
    private JDA jda;
    private int shardId;
    private int shardScale;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    
    private AmybotShard() {
        getLogger().info("Starting up new amybot shard...");
    }
    
    public static void main(final String[] args) {
        new AmybotShard().start();
    }
    
    private void start() {
        /*
         * Order of things is something like:
         * - Start container
         * - Derive shard ID from metadata
         * - Set up send / recv. queues
         * - Actually boot shard
         */
        eventBus.register(this);
        eventBus.post(InternalEvent.GET_SHARD_ID);
    }
    
    @Subscribe
    @SuppressWarnings("ConstantConditions")
    public void getShardId(final InternalEvent event) {
        if(event == InternalEvent.GET_SHARD_ID) {
            getLogger().info("Deriving shard numbers from Rancher...");
            try {
                final String serviceIndex = client.newCall(new Request.Builder()
                        .url("http://rancher-metadata/2015-12-19/self/container/service_index")
                        .build()).execute().body().string();
                final String serviceName = client.newCall(new Request.Builder()
                        .url("http://rancher-metadata/2015-12-19/self/container/service_name")
                        .build()).execute().body().string();
                final String serviceScale = client.newCall(new Request.Builder()
                        .url(String.format("http://rancher-metadata/2015-12-19/services/%s/scale", serviceName))
                        .build()).execute().body().string();
                // 12 containers -> 0 - 11 IDs
                shardId = Integer.parseInt(serviceIndex) - 1;
                shardScale = Integer.parseInt(serviceScale);
                eventBus.post(InternalEvent.QUEUE_CONNECT);
            } catch(final IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Subscribe
    public void handleDiscordEvent(final WrappedEvent event) {
        if(channel != null) {
            try {
                // TODO: Convert wrapped events into bytes
                final ObjectMapper mapper = new ObjectMapper();
                channel.basicPublish("", String.format(GATEWAY_QUEUE, shardId, shardScale), null,
                        // In THEORY this works?
                        mapper.writeValueAsString(event).getBytes());
            } catch(final IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Subscribe
    public void connectQueues(final InternalEvent event) {
        if(event == InternalEvent.QUEUE_CONNECT) {
            factory = new ConnectionFactory();
            factory.setHost(Optional.of(System.getenv("RABBITMQ_HOST")).orElse("rabbitmq"));
            try {
                // Connect
                connection = factory.newConnection();
                channel = connection.createChannel();
                
                // Declare I/O queues
                channel.queueDeclare(String.format(RABBITMQ_QUEUE_FORMAT, shardId, shardScale), false,
                        false, false, null);
                channel.queueDeclare(GATEWAY_QUEUE, false,
                        false, false, null);
                
                channel.basicConsume(String.format(RABBITMQ_QUEUE_FORMAT, shardId, shardScale), true,
                        new ShardQueueConsumer(this, channel));
                
                // Attempt to cleanly shut down
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        channel.close();
                        connection.close();
                    } catch(final IOException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }));
            } catch(final IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }
    
    void startBot(final int shardId, final int shardCount) {
        try {
            // TODO: Poll token bucket until we're allowed to connect
            // TODO: Build networked SessionReconnectQueue(?)
            jda = new JDABuilder(AccountType.BOT)
                    .useSharding(shardId, shardCount)
                    .setToken(System.getenv("BOT_TOKEN"))
                    .addEventListener((EventListener) event -> {
                        if(event instanceof ReadyEvent) {
                            // TODO: Probably wanna give people another way to set this
                            jda.getPresence().setGame(Game.of(jda.getSelfUser().getName() + " shard " + shardId + " / " + shardCount));
                            getLogger().info("Logged in as shard " + shardId + " / " + shardCount);
                        }
                    })
                    .buildAsync();
        } catch(final LoginException | RateLimitedException e) {
            e.printStackTrace();
        }
    }
    
    public enum InternalEvent {
        GET_SHARD_ID,
        QUEUE_CONNECT,
        READY,
        DISCONNECT,
    }
}

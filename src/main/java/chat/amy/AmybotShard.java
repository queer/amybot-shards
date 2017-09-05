package chat.amy;

import chat.amy.gateway.GatewayConnection;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 9/2/17.
 */
@SuppressWarnings({"unused", "UnnecessarilyQualifiedInnerClassAccess", "WeakerAccess"})
public final class AmybotShard {
    public static final String SHARD_ROOT = "/amybot/shard";
    public static final String SHARD_ID_SEMAPHORE = SHARD_ROOT + "/id";
    @Getter
    @SuppressWarnings("TypeMayBeWeakened")
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build();
    @Getter
    private final Logger logger = LoggerFactory.getLogger("amybot-shard");
    @Getter
    private final GatewayConnection gatewayConnection = new GatewayConnection(this);
    @Getter
    private final EventBus eventBus = new EventBus();
    @Getter
    private JDA jda;
    
    private AmybotShard() {
        getLogger().info("Starting up new amybot shard...");
    }
    
    public static void main(final String[] args) {
        new AmybotShard().start();
    }
    
    private void start() {
        eventBus.register(this);
        gatewayConnection.connect();
    }
    
    @Subscribe
    @SuppressWarnings("ConstantConditions")
    public void onReady(final InternalEvent event) {
        if(event == InternalEvent.READY) {
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
                startBot(Integer.parseInt(serviceIndex) - 1, Integer.parseInt(serviceScale));
            } catch(final IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    void startBot(final int shardId, final int shardCount) {
        try {
            // TODO: Ratelimit this. Need to come up with a distributed ratelimiter. ZK/Consul impl.?
            jda = new JDABuilder(AccountType.BOT)
                    .useSharding(shardId, shardCount)
                    .setToken(System.getenv("BOT_TOKEN"))
                    .addEventListener((EventListener) event -> {
                        if(event instanceof ReadyEvent) {
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
        READY,
        DISCONNECT,
    }
}

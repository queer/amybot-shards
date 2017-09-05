package chat.amy;

import chat.amy.gateway.GatewayConnection;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import okhttp3.OkHttpClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.framework.recipes.locks.Lease;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
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
    @Getter
    private CuratorFramework framework;
    @Getter
    private InterProcessSemaphoreV2 semaphore;
    private Lease lease;
    
    private AmybotShard() {
        getLogger().info("Starting up new amybot shard...");
    }
    
    public static void main(final String[] args) {
        new AmybotShard().start();
    }
    
    private void start() {
        gatewayConnection.connect();
    }
    
    @Subscribe
    public void onReady(final InternalEvent event) {
        if(event == InternalEvent.READY) {
            getLogger().info("Setting up ZooKeeper...");
            framework = CuratorFrameworkFactory.newClient("127.0.0.1:2181", new ExponentialBackoffRetry(1000, 3));
            // TODO: Grab lease count somehow :C
            semaphore = new InterProcessSemaphoreV2(framework, SHARD_ID_SEMAPHORE, 2);
            try {
                lease = semaphore.acquire();
                getLogger().info("Got lease!");
                getLogger().info("Lease data: '" + Arrays.toString(lease.getData()) + "'.");
            } catch(final Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    void startBot(final int shardId, final int shardCount) {
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .useSharding(shardId, shardCount)
                    .setToken(System.getenv("BOT_TOKEN"))
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

package chat.amy;

import chat.amy.message.EventMessenger;
import chat.amy.message.RedisMessenger;
import chat.amy.shard.EnvSharder;
import chat.amy.shard.RancherSharder;
import chat.amy.shard.Sharder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Optional;

/**
 * @author amy
 * @since 9/2/17.
 */
@SuppressWarnings({"unused", "UnnecessarilyQualifiedInnerClassAccess", "WeakerAccess", "FieldCanBeLocal"})
public final class AmybotShard {
    @Getter
    private static final EventBus eventBus = new EventBus();
    @SuppressWarnings("TypeMayBeWeakened")
    @Getter
    private final Logger logger = LoggerFactory.getLogger("amybot-shard");
    // TODO: Make this configurable or smth
    private final EventMessenger messenger = new RedisMessenger(this);
    @Getter
    private JDA jda;
    private int shardId;
    private int shardScale;
    
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
        eventBus.register(messenger);
        eventBus.post(InternalEvent.GET_SHARD_ID);
    }
    
    @Subscribe
    @SuppressWarnings("ConstantConditions")
    public void getShardId(final InternalEvent event) {
        if(event == InternalEvent.GET_SHARD_ID) {
            try {
                logger.info("Getting shard IDs...");
                final Sharder sharder;
                switch(Optional.ofNullable(System.getenv("SHARDING_METHOD")).orElse("rancher")) {
                    case "env":
                        sharder = new EnvSharder();
                        break;
                    case "rancher":
                    default:
                        sharder = new RancherSharder();
                        break;
                }
                shardId = sharder.getShardId();
                shardScale = sharder.getShardScale();
                logger.info("This is shard {}/{}", shardId, shardScale);
                logger.info("Snoozing to wait on VisualVM...");
                Thread.sleep(10000L);
                logger.info("Done snoozin'!");
                eventBus.post(InternalEvent.START_BOT);
            } catch(final Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    
    @Subscribe
    public void startBot(final InternalEvent ievent) {
        if(ievent == InternalEvent.START_BOT) {
            try {
                // TODO: Poll token bucket until we're allowed to connect
                // TODO: Build networked SessionReconnectQueue(?)
                jda = new JDABuilder(AccountType.BOT)
                        .useSharding(shardId, shardScale)
                        .setToken(System.getenv("BOT_TOKEN"))
                        .addEventListener((EventListener) event -> {
                            if(event instanceof ReadyEvent) {
                                if(System.getenv("GAME") == null) {
                                    jda.getPresence().setGame(Game.of(jda.getSelfUser().getName() + " shard " + shardId + " / " + shardScale));
                                } else {
                                    jda.getPresence().setGame(Game.of(System.getenv("GAME")));
                                }
                                getLogger().info("Logged in as shard " + shardId + " / " + shardScale);
                                eventBus.post(InternalEvent.READY);
                            }
                        })
                        .buildAsync();
            } catch(final LoginException | RateLimitedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public enum InternalEvent {
        GET_SHARD_ID,
        START_BOT,
        READY,
    }
}

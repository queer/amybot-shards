package chat.amy.message;

import chat.amy.jda.WrappedEvent;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Optional;

/**
 * @author amy
 * @since 9/22/17.
 */
public class RedisMessenger implements EventMessenger {
    private final RedissonClient redis;
    
    public RedisMessenger() {
        final Config config = new Config();
        config.useSingleServer().setAddress(Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("redis://redis:6379"))
                // Based on my bot heavily abusing redis as it is, high connection pool size is not a terrible idea.
                // NOTE: Current live implementation uses like 500 connections in the pool, so TEST TEST TEST
                // TODO: Determine better sizing
                .setConnectionPoolSize(128);
        redis = Redisson.create(config);
    }
    
    @Override
    public void queue(final WrappedEvent event) {
        final RBlockingQueue<WrappedEvent> eventQueue = redis.getBlockingQueue("discord-intake");
        try {
            eventQueue.add(event);
        } catch(final IllegalStateException e) {
            throw new IllegalStateException("Couldn't append to the event queue! This likely means that you have more than " +
                    "4294967295 queued events, which is a REALLY BAD THING!", e);
        } catch(final Exception e) {
            // TODO: Logging
        }
    }
    
    @Override
    public Optional<WrappedEvent> poll() {
        throw new UnsupportedOperationException("Shards should not be polling!");
    }
}

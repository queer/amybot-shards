package chat.amy.message;

import chat.amy.jda.WrappedEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * @author amy
 * @since 9/22/17.
 */
@SuppressWarnings("unused")
public class RedisMessenger implements EventMessenger {
    private final RedissonClient redis;
    
    private final List<WrappedEvent> preloadEventCache = new ArrayList<>();
    private final List<String> streamableGuilds = new ArrayList<>();
    private int streamedGuildCount = 0;
    private boolean isStreamingGuilds = true;
    
    public RedisMessenger() {
        final Config config = new Config();
        config.useSingleServer().setAddress(Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("redis://redis:6379"))
                .setPassword(System.getenv("REDIS_PASS"))
                // Based on my bot heavily abusing redis as it is, high connection pool size is not a terrible idea.
                // NOTE: Current live implementation uses like 500 connections in the pool, so TEST TEST TEST
                // TODO: Determine better sizing
                .setConnectionPoolSize(128);
        redis = Redisson.create(config);
    }
    
    @Override
    public void queue(final WrappedEvent event) {
        // So this is actually a bit interesting. We need to not ship off events until we finish streaming all the guilds,
        // because otherwise the backend might not have caches available etc.
        // This is solved by
        // - Get READY event
        
        if(event.getType().equalsIgnoreCase("READY")) {
            // Discord READY event. Cache unavailable guilds, then start streaming
            final JSONArray guilds = event.getData().getJSONArray("guilds");
            StreamSupport.stream(guilds.spliterator(), false)
                    .map(JSONObject.class::cast).map(o -> o.getString("id")).forEach(streamableGuilds::add);
        } else if(event.getType().equalsIgnoreCase("GUILD_CREATE")) {
            // Cache the guild
            // TODO: Redisson cache
            if(isStreamingGuilds) {
                final String id = event.getData().getString("id");
                // If we're streaming guilds, we need to make sure we mark "finished" when we're done
                if(streamableGuilds.contains(id)) {
                    ++streamedGuildCount;
                    if(streamedGuildCount == streamableGuilds.size()) {
                        isStreamingGuilds = false;
                        preloadEventCache.forEach(this::queue);
                    }
                }
            }
        }
        if(isStreamingGuilds) {
            preloadEventCache.add(event);
            return;
        }
        
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

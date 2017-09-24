package chat.amy.message;

import chat.amy.cache.guild.Guild;
import chat.amy.jda.RawEvent;
import chat.amy.jda.WrappedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author amy
 * @since 9/22/17.
 */
@SuppressWarnings("unused")
public class RedisMessenger implements EventMessenger {
    private final RedissonClient redis;
    private final ObjectMapper mapper = new ObjectMapper();
    
    private final List<RawEvent> preloadEventCache = new ArrayList<>();
    private List<String> streamableGuilds;
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
    
    private <T> T readJson(String json, Class<T> c) {
        try {
            return mapper.readValue(json, c);
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void cacheGuild(RawEvent rawEvent) {
        // Cache the guild, and all its users at the same time
        // TODO: Should split this up, rather than cache a giant guild object
        Guild guild = readJson(rawEvent.getData().getJSONObject("d").toString(), Guild.class);
        final RBucket<Guild> bucket = redis.getBucket(String.format("guild:" + guild.getId() + ":bucket"));
        bucket.set(guild);
    
        // If we're streaming guilds, we need to make sure we mark "finished" when we're done
        if(isStreamingGuilds) {
            if(streamableGuilds.contains(guild.getId())) {
                ++streamedGuildCount;
                if(streamedGuildCount == streamableGuilds.size()) {
                    isStreamingGuilds = false;
                    preloadEventCache.forEach(this::queue);
                }
            }
        }
    }
    
    @Override
    @Subscribe
    public void queue(final RawEvent rawEvent) {
        // So this is actually a bit interesting. We need to not ship off events until we finish streaming all the guilds,
        // because otherwise the backend might not have caches available etc.
        // This is solved by
        // - Get READY event
        // - Start streaming guilds
        // - Cache non-guild events that come in while streaming
        // - When caching finishes, "replay" all events
        final String type = rawEvent.getData().getString("t");
        
        if(type.equalsIgnoreCase("READY")) {
            // Discord READY event. Cache unavailable guilds, then start streaming
            // TODO: Move to Jackson?
            final JSONArray guilds = rawEvent.getData().getJSONObject("d").getJSONArray("guilds");
            streamableGuilds = StreamSupport.stream(guilds.spliterator(), false)
                    .map(JSONObject.class::cast).map(o -> o.getString("id")).collect(Collectors.toList());
            return;
        } else if(type.equalsIgnoreCase("GUILD_CREATE")) {
            cacheGuild(rawEvent);
            return;
        } else if(type.equalsIgnoreCase("GUILD_DELETE")) {
            // Convert to a GUILD_CREATE for unavailability
            if(isStreamingGuilds && rawEvent.getData().getJSONObject("d").has("unavailable")
                    && rawEvent.getData().getJSONObject("d").getBoolean("unavailable")) {
                cacheGuild(rawEvent);
            } else {
                // Otherwise delet
                Guild guild = readJson(rawEvent.getData().getJSONObject("d").toString(), Guild.class);
                final RBucket<Guild> bucket = redis.getBucket(String.format("guild:" + guild.getId() + ":bucket"));
                bucket.delete();
            }
            return;
        }
        if(isStreamingGuilds) {
            preloadEventCache.add(rawEvent);
            return;
        }
        
        final RBlockingQueue<WrappedEvent> eventQueue = redis.getBlockingQueue("discord-intake");
        try {
            eventQueue.add(new WrappedEvent("discord", type, rawEvent.getData().getJSONObject("d")));
        } catch(final IllegalStateException e) {
            throw new IllegalStateException("Couldn't append to the event queue! This likely means that you have more than " +
                    "4294967295 queued events, which is a REALLY BAD THING!", e);
        } catch(final Exception e) {
            // TODO: Logging
        }
    }
    
    @Override
    public Optional<RawEvent> poll() {
        throw new UnsupportedOperationException("Shards should not be polling!");
    }
}

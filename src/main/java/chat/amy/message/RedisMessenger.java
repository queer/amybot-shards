package chat.amy.message;

import chat.amy.cache.guild.RawGuild;
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
        // Cache the rawGuild, and all its users at the same time
        // TODO: Should split this up, rather than cache a giant rawGuild object
        // Basically, we should just
        // - take the members list out
        // - convert the rawGuild's members list to a list of IDs
        // - Store the members list as a list of its own, IDs only(?)
        // - Store users in another list
        // - Probably store channels elsewhere as well? :V
        RawGuild rawGuild = readJson(rawEvent.getData().getJSONObject("d").toString(), RawGuild.class);
        final RBucket<RawGuild> bucket = redis.getBucket(String.format("guild:" + rawGuild.getId() + ":bucket"));
        bucket.set(rawGuild);
    
        // If we're streaming guilds, we need to make sure we mark "finished" when we're done
        if(isStreamingGuilds) {
            if(streamableGuilds.contains(rawGuild.getId())) {
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
        //
        // As it turns out, there's actually good reason to not do this in the gateway nodes instead. This is done inside
        // of the shard process because we need to be sure that events that are cached here actually have all the data
        // available. The problem is that when we stream guilds from Discord, there is a possibility that we recv. events
        // during that streaming period.
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
                RawGuild rawGuild = readJson(rawEvent.getData().getJSONObject("d").toString(), RawGuild.class);
                final RBucket<RawGuild> bucket = redis.getBucket(String.format("guild:" + rawGuild.getId() + ":bucket"));
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

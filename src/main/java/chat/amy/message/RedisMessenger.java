package chat.amy.message;

import chat.amy.AmybotShard;
import chat.amy.cache.context.CacheContext;
import chat.amy.cache.guild.Guild;
import chat.amy.cache.raw.RawGuild;
import chat.amy.jda.RawEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author amy
 * @since 9/22/17.
 */
@SuppressWarnings("FieldCanBeLocal")
public class RedisMessenger implements EventMessenger {
    private final AmybotShard shard;
    private final JedisPool redis;
    private final Collection<RawEvent> preloadEventCache = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger("Messenger");
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private List<String> streamableGuilds;
    private int streamedGuildCount;
    private boolean isStreamingGuilds = true;
    private long start;
    private long end;
    
    public RedisMessenger(final AmybotShard shard) {
        this.shard = shard;
        final JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(1024);
        jedisPoolConfig.setMaxTotal(1024);
        jedisPoolConfig.setMaxWaitMillis(500);
        redis = new JedisPool(jedisPoolConfig, Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("redis"));
    }
    
    private void cache(final Consumer<Jedis> op) {
        try(Jedis jedis = redis.getResource()) {
            jedis.auth(System.getenv("REDIS_PASS"));
            op.accept(jedis);
        }
    }
    
    private <T> T fromJson(final String json, final Class<T> c) {
        try {
            return mapper.readValue(json, c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the <code>d</code> field of the event and parse that into an object.
     *
     * @param event Event to parse
     * @param c     Class to parse to
     * @param <T>   Type to parse to
     *
     * @return Parsed object
     */
    private <T> T readJson(final RawEvent event, final Class<T> c) {
        try {
            final JsonNode tree = mapper.readTree(event.getRaw());
            return mapper.treeToValue(tree.get("d"), c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("ConstantConditions")
    private void cacheGuild(final RawEvent rawEvent) {
        final RawGuild rawGuild = readJson(rawEvent, RawGuild.class);
        final Guild guild = Guild.fromRaw(rawGuild);
        
        // TODO: It's probably a better idea to bucket stuff into hashes, but that spreads data around more :I
        // This seems Fast Enough:tm: for now; prod. shard boots about 3 seconds after finishing the login,
        // which is probably good enough for this.
        //noinspection CodeBlock2Expr
        pool.execute(() -> {
            // Bucket the guild itself
            guild.cache(new CacheContext<>(redis));
            // Bucket channels, members, roles, ...
            rawGuild.cache(new CacheContext<>(redis));
        });
        
        // If we're streaming guilds, we need to make sure we mark "finished" when we're done
        if(isStreamingGuilds) {
            if(streamableGuilds.contains(rawGuild.getId())) {
                ++streamedGuildCount;
                if(streamedGuildCount == streamableGuilds.size()) {
                    isStreamingGuilds = false;
                    end = System.currentTimeMillis();
                    cache(jedis -> {
                        logger.info("Started up in " + (end - start) + "ms");
                        logger.info("Our caches vs JDA:");
                        logger.info("Guilds:  {} vs {}", jedis.scard("guild:sset"), shard.getJda().getGuildCache().size());
                        logger.info("Users:   {} vs {}", jedis.scard("user:sset"), shard.getJda().getUserCache().size());
                    });
                    preloadEventCache.forEach(this::queue);
                }
            }
        }
    }
    
    @SuppressWarnings("ConstantConditions")
    @Override
    @Subscribe
    public void queue(final RawEvent rawEvent) {
        // TODO: This logic should probably exist somewhere else...
        
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
    
        switch(type) {
            case "READY":
                // Discord READY event. Cache unavailable guilds, then start streaming
                // TODO: Move to Jackson?
                final JSONArray guilds = rawEvent.getData().getJSONObject("d").getJSONArray("guilds");
                streamableGuilds = StreamSupport.stream(guilds.spliterator(), false)
                        .map(JSONObject.class::cast).map(o -> o.getString("id")).collect(Collectors.toList());
                start = System.currentTimeMillis();
                return;
            case "GUILD_CREATE":
                cacheGuild(rawEvent);
                return;
            case "GUILD_DELETE":
                // Convert to a GUILD_CREATE for unavailability
                if(isStreamingGuilds && rawEvent.getData().getJSONObject("d").has("unavailable")
                        && rawEvent.getData().getJSONObject("d").getBoolean("unavailable")) {
                    cacheGuild(rawEvent);
                } else {
                    // Otherwise delet
                    //noinspection CodeBlock2Expr
                    cache(jedis -> {
                        pool.execute(() -> {
                            final RawGuild rawGuild = readJson(rawEvent, RawGuild.class);
                            final Guild guild = fromJson(jedis.get("guild:" + rawGuild.getId() + ":bucket"), Guild.class);
                            rawGuild.uncache(new CacheContext<>(redis));
                            guild.uncache(new CacheContext<>(redis));
                        });
                    });
                }
                return;
            case "GUILD_MEMBERS_CHUNK":
                // TODO: Cache these
                break;
        }
        if(isStreamingGuilds) {
            preloadEventCache.add(rawEvent);
            return;
        }
        
        // Append raw event JSON to the end of the queue
        cache(jedis -> jedis.rpush("discord-intake", rawEvent.getRaw()));
    }
    
    @Override
    public Optional<RawEvent> poll() {
        throw new UnsupportedOperationException("Shards should not be polling!");
    }
}

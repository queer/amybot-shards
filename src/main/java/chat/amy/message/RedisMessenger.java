package chat.amy.message;

import chat.amy.cache.guild.Guild;
import chat.amy.cache.guild.Member;
import chat.amy.cache.raw.RawGuild;
import chat.amy.cache.user.User;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author amy
 * @since 9/22/17.
 */
@SuppressWarnings("unused")
public class RedisMessenger implements EventMessenger {
    private final JedisPool redis;
    private final Collection<RawEvent> preloadEventCache = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private List<String> streamableGuilds;
    private int streamedGuildCount;
    private boolean isStreamingGuilds = true;
    
    private final Logger logger = LoggerFactory.getLogger("Messenger");
    
    public RedisMessenger() {
        final JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(1024);
        jedisPoolConfig.setMaxTotal(1024);
        jedisPoolConfig.setMaxWaitMillis(500);
        redis = new JedisPool(jedisPoolConfig, Optional.ofNullable(System.getenv("REDIS_HOST")).orElse("redis"));
    }
    
    private <T> T readJson(final String json, final Class<T> c) {
        try {
            return mapper.readValue(json, c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void cache(final Consumer<Jedis> op) {
        try(Jedis jedis = redis.getResource()) {
            jedis.auth(System.getenv("REDIS_PASS"));
            op.accept(jedis);
        }
    }
    
    private <T> String toJson(final T o) {
        try {
            return mapper.writeValueAsString(o);
        } catch(final Exception e) {
            throw new RuntimeException(e);
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
        
        cache(jedis -> {
            // Bucket the guild
            logger.debug("Caching guild: {}", guild.getId());
            jedis.set("guild:" + guild.getId() + ":bucket", toJson(guild));
            jedis.sadd("guild:sset", guild.getId());
            logger.debug("Bucketed guild");
            // Bucket each channel
            rawGuild.getChannels().forEach(channel -> {
                jedis.set("channel:" + channel.getId() + ":bucket", toJson(channel));
                jedis.sadd("channel:sset", channel.getId());
                logger.debug("Bucketed channel: {}", channel.getId());
                // TODO: Bucket into categories, voice/text, ...
            });
            // Bucket the users, overwriting old users
            rawGuild.getMembers().forEach(member -> {
                final User user = member.getUser();
                // Bucket members
                jedis.set("member:" + guild.getId() + ':' + user.getId() + ":bucket", toJson(Member.fromRaw(member)));
                // Bucket users
                jedis.set("user:" + user.getId() + ":bucket", toJson(user));
                // Bucket user ID
                jedis.sadd("user:sset", user.getId());
                logger.debug("Bucketed user: {}", user.getId());
            });
            logger.debug("Finished caching guild: {}", guild.getId());
        });
        
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
                cache(jedis -> {
                    final RawGuild rawGuild = readJson(rawEvent, RawGuild.class);
                    // Get the real guild from the cache
                    final Guild guild = fromJson(jedis.get("guild:" + rawGuild.getId() + ":bucket"), Guild.class);
                    // Nuke channels
                    jedis.del(guild.getChannels().stream().map(e -> "channel:" + e.getId() + ":bucket").toArray(String[]::new));
                    // Nuke members
                    jedis.del(guild.getMembers().stream()
                            .map(e -> "member:" + rawGuild.getId() + ':' + e.getUserId() + ":bucket").toArray(String[]::new));
                    // Nuke the full guild
                    jedis.del("guild:" + guild.getId() + ":bucket");
                    jedis.srem("guild:sset", guild.getId());
                    // Nuke expired users in the cache
                    final Set<String> oldGuilds = jedis.smembers("guild:sset");
                    // Get list of nuked guild's users
                    final List<String> memberIds = guild.getMembers().stream().map(Member::getUserId).collect(Collectors.toList());
                    // Check against members in every other guild
                    oldGuilds.forEach(e -> {
                        final Guild other = fromJson(jedis.get("guild:" + e + ":bucket"), Guild.class);
                        // Remove old members
                        other.getMembers().forEach(m -> memberIds.remove(m.getUserId()));
                    });
                    // For those who survive, delete them
                    memberIds.forEach(e -> {
                        jedis.del("user:" + e + ":bucket");
                        jedis.srem("user:sset", e);
                    });
                });
            }
            return;
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

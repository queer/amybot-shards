package chat.amy.discord;

import chat.amy.AmybotShard;
import chat.amy.discord.handle.GuildCreateHandler;
import chat.amy.discord.handle.GuildDeleteHandler;
import chat.amy.discord.handle.GuildMembersChunkHandler;
import chat.amy.discord.handle.ReadyHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The irony, of course, is that we just end up duplicating a lot of the
 * effort that went into JDA :^)
 *
 * @author amy
 * @since 10/3/17.
 */
public class WSEventManager {
    @Getter
    private final AmybotShard shard;
    @Getter
    private final Queue<RawEvent> preloadEventCache = new ConcurrentLinkedQueue<>();
    private final ObjectMapper mapper = new ObjectMapper();
    @Getter
    private final ExecutorService pool = Executors.newCachedThreadPool();
    @Getter
    private final Map<String, WSEventHandler<?>> handlers = new HashMap<>();
    @Getter
    private final Map<String, WSEventHandler<JedisPool>> cacheHandlers = new HashMap<>();
    @Getter
    private List<String> streamableGuilds = new ArrayList<>();
    @Getter
    @Setter
    private int streamedGuildCount;
    @Getter
    @Setter
    private boolean isStreamingGuilds = true;
    @Getter
    @Setter
    private long start;
    @Getter
    @Setter
    private boolean ready;
    private boolean setup;
    
    public WSEventManager(final AmybotShard shard) {
        this.shard = shard;
    }
    
    public void incrementStreamedGuilds() {
        streamedGuildCount += 1;
    }
    
    public void setup() {
        if(!setup) {
            setup = true;
            handlers.put("READY", new ReadyHandler());
            cacheHandlers.put("GUILD_CREATE", new GuildCreateHandler());
            cacheHandlers.put("GUILD_DELETE", new GuildDeleteHandler());
            cacheHandlers.put("GUILD_MEMBERS_CHUNK", new GuildMembersChunkHandler());
        }
    }
    
    @Subscribe
    public void handle(final RawEvent rawEvent) {
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
        String type = rawEvent.getData().getString("t");
        final JSONObject data = rawEvent.getData().getJSONObject("d");
        
        // If a guild is "unavailable" while streaming guilds, and it's a GUILD_DELETE, convert it to a GUILD_CREATE event
        // When this happens, it's probably because a guild went unavailable or smth during a Discord outage
        if(isStreamingGuilds && data.has("unavailable")
                && data.getBoolean("unavailable")) {
            type = "GUILD_CREATE";
        }
        
        switch(type) {
            case "READY": {
                handlers.get(type).handle(new WSEventContext<>(this, rawEvent));
                return;
            }
            case "GUILD_MEMBERS_CHUNK":
            case "GUILD_DELETE":
            case "GUILD_CREATE": {
                cacheHandlers.get(type).handle(new CachedEventContext(this, rawEvent, shard.getRedis()));
                // Don't queue these events for the backend if we're not ready
                if(isStreamingGuilds) {
                    return;
                }
                break;
            }
            case "CHANNEL_CREATE":
            case "CHANNEL_UPDATE":
            case "CHANNEL_DELETE":
            case "GUILD_UPDATE":
            case "GUILD_EMOJIS_UPDATE":
            case "GUILD_MEMBER_ADD":
            case "GUILD_MEMBER_REMOVE":
            case "GUILD_MEMBER_UPDATE":
            case "GUILD_ROLE_CREATE":
            case "GUILD_ROLE_UPDATE":
            case "GUILD_ROLE_DELETE":
            case "MESSAGE_CREATE":
            // TODO: Give a fuck about reactions?
            case "PRESENCE_UPDATE":
            case "USER_UPDATE":
            // TODO: Give a fuck about voice state updates?
            default:
                break;
        }
        
        // If we're streaming guilds, cache the event as needed
        if(isStreamingGuilds) {
            preloadEventCache.add(rawEvent);
        } else {
            shard.getMessenger().queue(rawEvent);
        }
    }
    
    private <T> T fromJson(final String json, final Class<T> c) {
        try {
            return mapper.readValue(json, c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}

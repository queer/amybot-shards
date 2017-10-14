package chat.amy.discord;

import chat.amy.AmybotShard;
import chat.amy.discord.handle.MessageCreateHandler;
import chat.amy.discord.handle.ReadyHandler;
import chat.amy.discord.handle.guild.GuildCreateHandler;
import chat.amy.discord.handle.guild.GuildDeleteHandler;
import chat.amy.discord.handle.guild.GuildMembersChunkHandler;
import chat.amy.discord.handle.guild.GuildUpdateHandler;
import chat.amy.discord.handle.guild.channel.ChannelCreateHandler;
import chat.amy.discord.handle.guild.channel.ChannelDeleteHandler;
import chat.amy.discord.handle.guild.channel.ChannelUpdateHandler;
import chat.amy.discord.handle.guild.emote.GuildEmoteUpdateHandler;
import chat.amy.discord.handle.guild.member.GuildMemberAddHandler;
import chat.amy.discord.handle.guild.member.GuildMemberRemoveHandler;
import chat.amy.discord.handle.guild.member.GuildMemberUpdateHandler;
import chat.amy.discord.handle.guild.role.GuildRoleCreateHandler;
import chat.amy.discord.handle.guild.role.GuildRoleDeleteHandler;
import chat.amy.discord.handle.guild.role.GuildRoleUpdateHandler;
import chat.amy.discord.handle.user.PresenceUpdateHandler;
import chat.amy.discord.handle.user.UserUpdateHandler;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import redis.clients.jedis.JedisPool;

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
@SuppressWarnings("OverlyCoupledClass")
public class WSEventManager {
    @Getter
    private final AmybotShard shard;
    @Getter
    @SuppressWarnings("TypeMayBeWeakened")
    private final Queue<RawEvent> preloadEventCache = new ConcurrentLinkedQueue<>();
    @Getter
    private final ExecutorService pool = Executors.newCachedThreadPool();
    @Getter
    private final Map<String, WSEventHandler<?>> handlers = new HashMap<>();
    @Getter
    private final Map<String, WSEventHandler<JedisPool>> cacheHandlers = new HashMap<>();
    @Getter
    private final List<String> streamableGuilds = new ArrayList<>();
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
            cacheHandlers.put("CHANNEL_CREATE", new ChannelCreateHandler());
            cacheHandlers.put("CHANNEL_UPDATE", new ChannelUpdateHandler());
            cacheHandlers.put("CHANNEL_DELETE", new ChannelDeleteHandler());
            cacheHandlers.put("GUILD_UPDATE", new GuildUpdateHandler());
            cacheHandlers.put("GUILD_EMOJIS_UPDATE", new GuildEmoteUpdateHandler());
            cacheHandlers.put("GUILD_MEMBER_ADD", new GuildMemberAddHandler());
            cacheHandlers.put("GUILD_MEMBER_REMOVE", new GuildMemberRemoveHandler());
            cacheHandlers.put("GUILD_MEMBER_UPDATE", new GuildMemberUpdateHandler());
            cacheHandlers.put("GUILD_ROLE_CREATE", new GuildRoleCreateHandler());
            cacheHandlers.put("GUILD_ROLE_UPDATE", new GuildRoleUpdateHandler());
            cacheHandlers.put("GUILD_ROLE_DELETE", new GuildRoleDeleteHandler());
            cacheHandlers.put("MESSAGE_CREATE", new MessageCreateHandler());
            cacheHandlers.put("PRESENCE_UPDATE", new PresenceUpdateHandler());
            cacheHandlers.put("USER_UPDATE", new UserUpdateHandler());
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
                cacheHandlers.get(type).handle(new CachedEventContext(this, rawEvent, shard.getRedis()));
                break;
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
}

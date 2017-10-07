package chat.amy.cache.guild;

import chat.amy.cache.Snowflake;
import chat.amy.cache.context.CacheContext;
import chat.amy.cache.CachedObject;
import chat.amy.cache.context.CacheReadContext;
import chat.amy.cache.presence.PresenceUpdate;
import chat.amy.cache.raw.RawGuild;
import chat.amy.cache.voice.VoiceState;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 9/24/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public final class Guild implements CachedObject<Void>, Snowflake {
    /**
     * Only sent during GUILD_CREATE
     */
    private final List<Member> members;
    /**
     * Only sent during GUILD_CREATE
     */
    private final List<Channel> channels;
    /**
     * Only sent during GUILD_CREATE
     */
    private final List<PresenceUpdate> presences;
    private String id;
    private String name;
    private String icon;
    private String splash;
    @JsonProperty("owner_id")
    private String ownerId;
    private String region;
    @JsonProperty("afk_channel_id")
    private String afkChannelId;
    @JsonProperty("afk_timeout")
    private int afkTimeout;
    @JsonProperty("embed_enabled")
    private boolean embedEnabled;
    @JsonProperty("embed_channel_id")
    private String embedChannelId;
    @JsonProperty("verification_level")
    private int verificationLevel;
    @JsonProperty("default_message_notifications")
    private int defaultMessageNotifications;
    @JsonProperty("explicit_content_filter")
    private int explicitContentFilter;
    private List<Role> roles;
    private List<Emote> emojis;
    private List<String> features;
    @JsonProperty("mfa_level")
    private int mfaLevel;
    @JsonProperty("application_id")
    private String applicationId;
    @JsonProperty("widget_enabled")
    private boolean widgetEnabled;
    @JsonProperty("widget_channel_id")
    private String widgetChannelId;
    /**
     * Only sent during GUILD_CREATE
     */
    @JsonProperty("joined_at")
    private String joinedAt;
    /**
     * Only sent during GUILD_CREATE
     */
    private boolean large;
    /**
     * Only sent during GUILD_CREATE
     */
    private boolean unavailable;
    /**
     * Only sent during GUILD_CREATE
     */
    @JsonProperty("member_count")
    private int memberCount;
    /**
     * Only sent during GUILD_CREATE
     */
    @JsonProperty("voice_states")
    private List<VoiceState> voiceStates;
    
    private Guild() {
        members = new CopyOnWriteArrayList<>();
        channels = new CopyOnWriteArrayList<>();
        presences = new CopyOnWriteArrayList<>();
    }
    
    public static Guild fromRaw(final RawGuild r) {
        final Guild g = new Guild();
        g.members.addAll(r.getMembers().stream().map(Member::fromRaw).collect(Collectors.toList()));
        g.channels.addAll(r.getChannels());
        g.presences.addAll(r.getPresences().stream().map(PresenceUpdate::fromRaw).collect(Collectors.toList()));
        g.id = r.getId();
        g.name = r.getName();
        g.icon = r.getIcon();
        g.splash = r.getSplash();
        g.ownerId = r.getOwnerId();
        g.region = r.getRegion();
        g.verificationLevel = r.getVerificationLevel();
        g.defaultMessageNotifications = r.getDefaultMessageNotifications();
        g.explicitContentFilter = r.getExplicitContentFilter();
        g.roles = r.getRoles();
        g.emojis = r.getEmojis();
        g.features = r.getFeatures();
        g.mfaLevel = r.getMfaLevel();
        g.applicationId = r.getApplicationId();
        g.widgetEnabled = r.isWidgetEnabled();
        g.widgetChannelId = r.getWidgetChannelId();
        g.joinedAt = r.getJoinedAt();
        g.large = r.isLarge();
        g.unavailable = r.isUnavailable();
        g.memberCount = r.getMemberCount();
        g.voiceStates = r.getVoiceStates();
        
        return g;
    }
    
    @Override
    public void cache(final CacheContext<Void> context) {
        context.cache(jedis -> {
            jedis.set("guild:" + getId() + ":bucket", toJson(this));
            jedis.sadd("guild:sset", getId());
        });
    }
    
    @Override
    public void uncache(final CacheContext<Void> context) {
        context.cache(jedis -> {
            // Remove ourselves from the cache
            jedis.del("guild:" + getId() + ":bucket", toJson(this));
            jedis.srem("guild:sset", getId());
            // Find if any of our users are still needed, and delete the ones that aren't
            final Set<String> oldGuilds = jedis.smembers("guild:sset");
            // Get list of nuked guild's users
            final List<String> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());
            // Check against members in every other guild
            oldGuilds.forEach(e -> {
                final Guild other = CachedObject.cacheRead(CacheReadContext.fromContext(context, "guild:" + e + ":bucket", Guild.class));
                //final Guild other = readJson(jedis.get("guild:" + e + ":bucket"), Guild.class);
                // Remove old members
                other.getMembers().forEach(m -> memberIds.remove(m.getId()));
            });
            // For those who survive, delete them
            memberIds.forEach(e -> {
                jedis.del("user:" + e + ":bucket");
                jedis.srem("user:sset", e);
            });
        });
    }
}


package chat.amy.cache.raw;

import chat.amy.cache.guild.Channel;
import chat.amy.cache.guild.Emote;
import chat.amy.cache.guild.Role;
import chat.amy.cache.voice.VoiceState;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
public class RawGuild {
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
    /**
     * Only sent during GUILD_CREATE
     */
    private final List<RawMember> members;
    /**
     * Only sent during GUILD_CREATE
     */
    private final List<Channel> channels;
    /**
     * Only sent during GUILD_CREATE
     */
    private final List<RawPresenceUpdate> presences;
    @JsonProperty("system_channel_id")
    private String systemChannelId;
}

package chat.amy.cache.voice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoiceState {
    @JsonProperty("guild_id")
    private String guildId;
    @JsonProperty("channel_id")
    private String channelId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("session_id")
    private String sessionId;
    private boolean deaf;
    private boolean mute;
    @JsonProperty("self_deaf")
    private String selfDeaf;
    @JsonProperty("self_mute")
    private String selfMute;
    private boolean suppress;
    @JsonProperty("self_video")
    private String selfVideo;
}

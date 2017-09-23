package chat.amy.cache.guild;

import chat.amy.cache.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
public class Channel {
    private String id;
    private ChannelType type;
    @JsonProperty("guild_id")
    private String guildId;
    private int position;
    @JsonProperty("permission_overwrites")
    private List<PermissionOverwrite> permissionOverwrites;
    private String name;
    private String topic;
    @JsonProperty("last_message_id")
    private String lastMessageId;
    /**
     * Voice channels only
     */
    private int bitrate;
    @JsonProperty("user_limit")
    private int userLimit;
    private List<User> recipients;
    private String icon;
    @JsonProperty("owner_id")
    private String ownerId;
    @JsonProperty("application_id")
    private String applicationId;
    /**
     * For categories
     */
    @JsonProperty("parent_id")
    private String parentId;
    
    public enum ChannelType {
        GUILD_TEXT(0),
        DM(1),
        GUILD_VOICE(2),
        GROUP_DM(3),
        CATEGORY(4);
        
        @Getter
        private final int type;
    
        ChannelType(final int type) {
            this.type = type;
        }
        
        @JsonValue
        public int toValue() {
            return type;
        }
    }
}

package chat.amy.cache.presence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import chat.amy.cache.raw.RawPresenceUpdate;

import java.util.List;

/**
 * @author amy
 * @since 9/25/17.
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresenceUpdate {
    private String userId;
    private List<String> roles;
    private Game game;
    @JsonProperty("guild_id")
    private String guildId;
    private String status;
    
    private PresenceUpdate() {
    }
    
    public static PresenceUpdate fromRaw(RawPresenceUpdate r) {
        PresenceUpdate p = new PresenceUpdate();
        p.userId = r.getUser().getId();
        p.roles = r.getRoles();
        p.game = r.getGame();
        p.guildId = r.getGuildId();
        p.status = r.getStatus();
        return p;
    }
}

package chat.amy.cache.raw;

import chat.amy.cache.presence.Game;
import chat.amy.cache.user.User;
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
public class RawPresenceUpdate {
    private User user;
    private List<String> roles;
    private Game game;
    @JsonProperty("guild_id")
    private String guildId;
    private String status;
}

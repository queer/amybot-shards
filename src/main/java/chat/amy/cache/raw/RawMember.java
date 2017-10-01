package chat.amy.cache.raw;

import chat.amy.cache.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawMember {
    private User user;
    private String nick;
    private List<String> roles;
    @JsonProperty("joined_at")
    private String joinedAt;
    private boolean deaf;
    private boolean mute;
}

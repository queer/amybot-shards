package chat.amy.cache.guild;

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
public class RawMember {
    private User user;
    private String nickname;
    private List<Role> roles;
    @JsonProperty("joined_at")
    private String joinedAt;
    private boolean deaf;
    private boolean mute;
}

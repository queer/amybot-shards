package chat.amy.cache.guild;

import chat.amy.cache.guild.raw.RawMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author amy
 * @since 9/24/17.
 */
@Data
@AllArgsConstructor
public class Member {
    private String userId;
    private String nickname;
    private List<Role> roles;
    @JsonProperty("joined_at")
    private String joinedAt;
    private boolean deaf;
    private boolean mute;
    
    private Member() {
    }
    
    public static Member fromRaw(final RawMember r) {
        final Member m = new Member();
        m.userId = r.getUser().getId();
        m.nickname = r.getNickname();
        m.roles = r.getRoles();
        m.joinedAt = r.getJoinedAt();
        m.deaf = r.isDeaf();
        m.mute = r.isMute();
        return m;
    }
}

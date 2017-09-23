package chat.amy.cache.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private String discrim;
    private String avatar;
    private boolean bot;
    @JsonProperty("mfa_enabled")
    private boolean mfaEnabled;
    private boolean verified;
    private String email;
}

package chat.amy.cache.user;

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
public class User {
    private String id;
    private String username;
    private String discriminator;
    private String avatar;
    private boolean bot;
    @JsonProperty("mfa_enabled")
    private boolean mfaEnabled;
    private boolean verified;
    private String email;
}

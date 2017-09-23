package chat.amy.cache.guild;

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
public class Emote {
    private String id;
    private String name;
    private List<Role> roles;
    @JsonProperty("require_colons")
    private boolean requireColons;
    private boolean managed;
}

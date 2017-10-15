package chat.amy.cache.guild;

import chat.amy.cache.Snowflake;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class Emote implements Snowflake {
    private String id;
    private String name;
    private List<Role> roles;
    @JsonProperty("require_colons")
    private boolean requireColons;
    private boolean managed;
}

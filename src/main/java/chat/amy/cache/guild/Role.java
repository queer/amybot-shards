package chat.amy.cache.guild;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class Role {
    private final String id;
    private String name;
    private int color;
    private boolean hoist;
    private int position;
    private int permissions;
    private boolean managed;
    private boolean mentionable;
}

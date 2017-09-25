package chat.amy.cache.guild;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
public class Role {
    private String id;
    private String name;
    private int color;
    private boolean hoist;
    private int position;
    private int permissions;
    private boolean managed;
    private boolean mentionable;
}

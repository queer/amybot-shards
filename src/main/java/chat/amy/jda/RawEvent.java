package chat.amy.jda;

import lombok.Value;
import org.json.JSONObject;

/**
 * @author amy
 * @since 9/23/17.
 */
@Value
public class RawEvent {
    private final String raw;
    private final JSONObject data;
}

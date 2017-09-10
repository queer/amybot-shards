package chat.amy.jda;

import lombok.Value;
import org.json.JSONObject;

/**
 * @author amy
 * @since 9/8/17.
 */
@Value
public class WrappedEvent {
    private final String type;
    private final JSONObject data;
}

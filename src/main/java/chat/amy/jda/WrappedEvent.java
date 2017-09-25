package chat.amy.jda;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import org.json.JSONObject;

/**
 * @author amy
 * @since 9/17/17.
 */
@Value
public class WrappedEvent {
    private final String source;
    private final String type;
    private final JsonNode data;
}

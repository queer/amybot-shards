package chat.amy.cache;

import chat.amy.discord.RawEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * @author amy
 * @since 10/3/17.
 */
public interface JsonCached {
    ObjectMapper mapper = new ObjectMapper();
    
    default <T> T readJsonEvent(final RawEvent event, final Class<T> c) {
        try {
            final JsonNode tree = mapper.readTree(event.getRaw());
            return mapper.treeToValue(tree.get("d"), c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    default <E> E readJson(final String json, final Class<E> c) {
        try {
            return mapper.readValue(json, c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    default <E> String toJson(final E o) {
        try {
            return mapper.writeValueAsString(o);
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

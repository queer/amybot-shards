package chat.amy.cache;

import chat.amy.jda.RawEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Interface for storing objects in the cache, as well as reading objects out.
 *
 * @author amy
 * @since 10/2/17.
 */
@FunctionalInterface
public interface CachedObject<T> {
    ObjectMapper mapper = new ObjectMapper();
    
    void cache(CacheContext<T> context);
    
    default <T> T readJson(final String json, final Class<T> c) {
        try {
            return mapper.readValue(json, c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    default <T> String toJson(final T o) {
        try {
            return mapper.writeValueAsString(o);
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the <code>d</code> field of the event and parse that into an object.
     *
     * @param event Event to parse
     * @param c     Class to parse to
     * @param <T>   Type to parse to
     *
     * @return Parsed object
     */
    default <T> T readJson(final RawEvent event, final Class<T> c) {
        try {
            final JsonNode tree = mapper.readTree(event.getRaw());
            return mapper.treeToValue(tree.get("d"), c);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}

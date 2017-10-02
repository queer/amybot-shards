package chat.amy.cache;

import chat.amy.cache.context.CacheContext;
import chat.amy.cache.context.CacheReadContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Interface for storing objects in the cache, as well as (probably) reading
 * objects out.
 *
 * @author amy
 * @since 10/2/17.
 */
public interface CachedObject<T> {
    ObjectMapper mapper = new ObjectMapper();
    
    static <E> E cacheRead(final CacheReadContext<String, Class<E>> context) {
        try {
            return mapper.readValue(context.getData(), context.getOtherData());
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Store this object in the cache
     *
     * @param context The context of the caching. Contains things like Redis
     *                pool connections.
     */
    void cache(CacheContext<T> context);
    
    /**
     * Delete this object from the cache
     *
     * @param context The context of the caching. Contains things like Redis
     *                pool connections.
     */
    void uncache(CacheContext<T> context);
    
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

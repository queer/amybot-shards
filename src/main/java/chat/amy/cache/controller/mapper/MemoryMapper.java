package chat.amy.cache.controller.mapper;

import chat.amy.cache.JsonCached;
import chat.amy.cache.Snowflake;
import chat.amy.cache.controller.CacheMapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The simplest possible mapper. Just maps everything to an in-memory
 * hashtable.
 *
 * This class is meant to be used ONLY for testing; do NOT rely on this for
 * actual production usage!
 *
 * @author amy
 * @since 10/5/17.
 */
// Somehow creating this class takes ~120ms. idk either. /shrug
public class MemoryMapper implements CacheMapper, JsonCached {
    /**
     * Map classes -> {snowflakes -> json data}
     */
    private final Map<Class<?>, Map<String, String>> cacheMap = new HashMap<>();
    
    @Override
    public <E extends Snowflake> void map(final E object) {
        if(!System.getenv().containsKey("TEST")) {
            System.err.println("WARNING: RUNNING MEMORYMAPPER IN NON-TEST ENV.");
        }
        if(!cacheMap.containsKey(object.getClass())) {
            cacheMap.put(object.getClass(), new HashMap<>());
        }
        // I'm pretty sure this'll work?
        cacheMap.get(object.getClass()).put(object.getId(), toJson(object));
    }
    
    @Override
    public <E extends Snowflake> void batchMap(final Collection<E> objects) {
        objects.forEach(this::map);
    }
    
    @Override
    public <E extends Snowflake> E unmap(final String snowflake, final Class<E> clz) {
        if(!System.getenv().containsKey("TEST")) {
            System.err.println("WARNING: RUNNING MEMORYMAPPER IN NON-TEST ENV.");
        }
        if(!cacheMap.containsKey(clz)) {
            return null;
        }
        return readJson(cacheMap.get(clz).get(snowflake), clz);
    }
}

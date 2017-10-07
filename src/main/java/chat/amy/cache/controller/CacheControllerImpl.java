package chat.amy.cache.controller;

import chat.amy.AmybotShard;
import chat.amy.cache.Snowflake;
import chat.amy.cache.controller.mapper.MemoryMapper;
import chat.amy.cache.controller.mapper.RedisMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author amy
 * @since 10/5/17.
 */
public class CacheControllerImpl implements CacheController {
    private final Map<Class<? extends Snowflake>, CacheMapper> configuredMappers = new ConcurrentHashMap<>();
    private boolean setup;
    private final CacheMapper defaultMapper;
    
    public CacheControllerImpl(final AmybotShard shard) {
        long start = System.currentTimeMillis();
        if(System.getenv().containsKey("TEST")) {
            defaultMapper = new MemoryMapper();
        } else {
            defaultMapper = new RedisMapper(shard);
        }
    }
    
    @Override
    public void setup() {
        if(setup) {
            return;
        }
        setup = true;
        // TODO: env configuration or some shit
    }
    
    @Override
    public <E extends Snowflake> CacheMapper getMapper(final Class<E> clz) {
        if(configuredMappers.containsKey(clz.getClass())) {
            return configuredMappers.get(clz.getClass());
        }
        return defaultMapper;
    }
}

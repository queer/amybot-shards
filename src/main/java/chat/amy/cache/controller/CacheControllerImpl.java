package chat.amy.cache.controller;

import chat.amy.AmybotShard;
import chat.amy.cache.Snowflake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author amy
 * @since 10/5/17.
 */
public class CacheControllerImpl implements CacheController {
    private final AmybotShard shard;
    private final Map<Class<? extends Snowflake>, CacheMapper> configuredMappers = new ConcurrentHashMap<>();
    private boolean setup;
    private final CacheMapper defaultMapper = null;
    
    public CacheControllerImpl(final AmybotShard shard) {
        this.shard = shard;
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
    public <E extends Snowflake> CacheMapper getMapper(final E object) {
        if(configuredMappers.containsKey(object.getClass())) {
            return configuredMappers.get(object.getClass());
        }
        return defaultMapper;
    }
}

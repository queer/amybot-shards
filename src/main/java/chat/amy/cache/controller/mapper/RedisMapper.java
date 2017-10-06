package chat.amy.cache.controller.mapper;

import chat.amy.AmybotShard;
import chat.amy.cache.JsonCached;
import chat.amy.cache.Snowflake;
import chat.amy.cache.controller.CacheMapper;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author amy
 * @since 10/5/17.
 */
public class RedisMapper implements CacheMapper, JsonCached {
    @Getter
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final AmybotShard shard;
    
    public RedisMapper(final AmybotShard shard) {
        this.shard = shard;
    }
    
    @Override
    public <E extends Snowflake> void map(final E object) {
        try(final Jedis jedis = shard.getRedis().getResource()) {
            jedis.set(object.getClass().getSimpleName().toLowerCase() + ':' + object.getId() + ":bucket", toJson(object));
        }
    }
    
    @Override
    public <E extends Snowflake> void batchMap(final Collection<E> objects) {
        // Batch operations are inherently gonna be a lot slower, since each command that we send to redis has to wait on the
        // response from the redis server in order to actually be ready to send the next command. The Correct Way:tm: to deal
        // with this is just pipelining the commands so they all get batch-executed.
        // See the "Pipelining" section here: https://github.com/xetorthio/jedis/wiki/AdvancedUsage
        
        try(final Jedis jedis = shard.getRedis().getResource()) {
            try(final Pipeline pipeline = jedis.pipelined()) {
                objects.forEach(object -> pipeline.set(object.getClass().getSimpleName().toLowerCase() + ':' + object.getId() + ":bucket", toJson(object)));
                // TODO: I *think* this is all I need to do? :Thonk:
                pipeline.sync();
            } catch(IOException e) {
                // TODO: Do I *really* wanna do this? ._.
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public <E extends Snowflake> E unmap(final String snowflake, final Class<E> clz) {
        try(final Jedis jedis = shard.getRedis().getResource()) {
            return readJson(jedis.get(clz.getSimpleName().toLowerCase() + ':' + snowflake + ":bucket"), clz);
        }
    }
}

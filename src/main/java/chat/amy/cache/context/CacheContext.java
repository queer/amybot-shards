package chat.amy.cache.context;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author amy
 * @since 10/2/17.
 */
public class CacheContext<T> {
    protected final JedisPool pool;
    @Getter
    private final T data;
    
    public CacheContext(final JedisPool pool) {
        this(pool, null);
    }
    
    public CacheContext(final JedisPool pool, final T data) {
        this.pool = pool;
        this.data = data;
    }
    
    public void cache(final Consumer<Jedis> function) {
        try(final Jedis jedis = pool.getResource()) {
            jedis.auth(System.getenv("REDIS_PASS"));
            function.accept(jedis);
        }
    }
    
    public static <E> CacheContext<E> fromContext(final CacheContext<?> ctx, final E data) {
        return new CacheContext<>(ctx.pool, data);
    }
}

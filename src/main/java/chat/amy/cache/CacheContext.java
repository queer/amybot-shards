package chat.amy.cache;

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
public final class CacheContext<T> {
    private final JedisPool pool;
    @Getter
    private final List<T> data;
    
    @SafeVarargs
    public CacheContext(final JedisPool pool, final T... data) {
        this.pool = pool;
        this.data = new ArrayList<>(Arrays.asList(data));
    }
    
    public void cache(final Consumer<Jedis> function) {
        try(final Jedis jedis = pool.getResource()) {
            function.accept(jedis);
        }
    }
    
    public static <E> CacheContext<E> fromContext(final CacheContext<?> ctx, final E... data) {
        return new CacheContext<>(ctx.pool, data);
    }
}

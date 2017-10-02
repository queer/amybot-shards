package chat.amy.cache.context;

import lombok.Getter;
import redis.clients.jedis.JedisPool;

/**
 * @author amy
 * @since 10/2/17.
 */
public class CacheReadContext<T, E> extends CacheContext<T> {
    @Getter
    private final E otherData;
    
    public CacheReadContext(final JedisPool pool, final T data, final E otherData) {
        super(pool, data);
        this.otherData = otherData;
    }
    
    public static <A, B> CacheReadContext<A, B> fromContext(final CacheContext<?> ctx, final A data, final B otherData) {
        return new CacheReadContext<>(ctx.pool, data, otherData);
    }
}

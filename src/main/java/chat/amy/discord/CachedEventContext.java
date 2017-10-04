package chat.amy.discord;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.Consumer;

/**
 * @author amy
 * @since 10/3/17.
 */
public class CachedEventContext extends WSEventContext<JedisPool> {
    public CachedEventContext(final WSEventManager wsEventManager, final RawEvent event, final JedisPool data) {
        super(wsEventManager, event, data);
    }
    
    public void cache(Consumer<Jedis> function) {
        getWsEventManager().getPool().execute(() -> {
            try(final Jedis jedis = getData().getResource()) {
                function.accept(jedis);
            }
        });
    }
}

package chat.amy.discord;

import redis.clients.jedis.JedisPool;

/**
 * @author amy
 * @since 10/3/17.
 */
public interface CachedEventHandler extends WSEventHandler<JedisPool> {
    @Override
    default void handle(WSEventContext<JedisPool> ctx) {
        if(ctx instanceof CachedEventContext) {
            handle((CachedEventContext) ctx);
        } else {
            ctx.getLogger().warn("Handler " + this + " got non-cached WSEventContext!?");
            ctx.getLogger().warn("Context: {}", ctx);
        }
    }
    
    void handle(CachedEventContext ctx);
}

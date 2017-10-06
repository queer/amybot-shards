package chat.amy.cache.guild;

import chat.amy.cache.Snowflake;
import chat.amy.cache.context.CacheContext;
import chat.amy.cache.CachedObject;
import chat.amy.cache.raw.RawGuild;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class Role implements CachedObject<RawGuild>, Snowflake {
    private final String id;
    private String name;
    private int color;
    private boolean hoist;
    private int position;
    private int permissions;
    private boolean managed;
    private boolean mentionable;
    
    @Override
    public void cache(final CacheContext<RawGuild> context) {
        context.cache(jedis -> {
            jedis.set("role:" + context.getData().getId() + ':' + id + ":bucket", toJson(this));
            jedis.sadd("role:sset", id);
        });
    }
    
    @Override
    public void uncache(final CacheContext<RawGuild> context) {
        context.cache(jedis -> {
            jedis.del("role:" + context.getData().getId() + ':' + id + ":bucket", toJson(this));
            jedis.srem("role:sset", id);
        });
    }
}

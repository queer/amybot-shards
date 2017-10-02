package chat.amy.cache.guild;

import chat.amy.cache.CacheContext;
import chat.amy.cache.CachedObject;
import chat.amy.cache.raw.RawGuild;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class Emote implements CachedObject<RawGuild> {
    private String id;
    private String name;
    private List<Role> roles;
    @JsonProperty("require_colons")
    private boolean requireColons;
    private boolean managed;
    
    @Override
    public void cache(final CacheContext<RawGuild> context) {
        context.cache(jedis -> {
            jedis.set("emote:" + context.getData().get(0).getId() + ':' + id + ":bucket", toJson(this));
            jedis.sadd("emote:sset", id);
        });
    }
    
    @Override
    public void uncache(final CacheContext<RawGuild> context) {
        context.cache(jedis -> {
            jedis.del("emote:" + context.getData().get(0).getId() + ':' + id + ":bucket", toJson(this));
            jedis.srem("emote:sset", id);
        });
    }
}

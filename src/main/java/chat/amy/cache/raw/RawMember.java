package chat.amy.cache.raw;

import chat.amy.cache.Snowflake;
import chat.amy.cache.context.CacheContext;
import chat.amy.cache.CachedObject;
import chat.amy.cache.guild.Member;
import chat.amy.cache.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawMember implements CachedObject<String>, Snowflake {
    private User user;
    private String nick;
    private List<String> roles;
    @JsonProperty("joined_at")
    private String joinedAt;
    private boolean deaf;
    private boolean mute;
    
    @Override
    public void cache(final CacheContext<String> context) {
        context.cache(jedis -> {
            final User user = getUser();
            // Bucket member
            jedis.set("member:" + context.getData() + ':' + user.getId() + ":bucket", toJson(Member.fromRaw(this)));
            user.cache(CacheContext.fromContext(context, null));
        });
    }
    
    @Override
    public void uncache(final CacheContext<String> context) {
        context.cache(jedis -> {
            final User user = getUser();
            // Bucket members
            jedis.del("member:" + context.getData() + ':' + user.getId() + ":bucket", toJson(Member.fromRaw(this)));
            // Don't delete the user bucket here because it might still be needed.
            // This is handled in Guild#uncache()
            /*if(jedis.exists("user:" + user.getId() + ":bucket")) {
                jedis.del("user:" + user.getId() + ":bucket", toJson(user));
                // Bucket user ID
                jedis.srem("user:sset", user.getId());
            }*/
        });
    }
    
    @Override
    public String getId() {
        return user.getId();
    }
}

package chat.amy.cache.user;

import chat.amy.cache.CachedObject;
import chat.amy.cache.context.CacheContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author amy
 * @since 9/23/17.
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements CachedObject<Void> {
    private String id;
    private String username;
    private String discriminator;
    private String avatar;
    private boolean bot;
    @JsonProperty("mfa_enabled")
    private boolean mfaEnabled;
    private boolean verified;
    private String email;
    
    @Override
    public void cache(final CacheContext<Void> context) {
        context.cache(jedis -> {
            // Bucket user
            if(!jedis.exists("user:" + getId() + ":bucket")) {
                jedis.set("user:" + getId() + ":bucket", toJson(this));
                // Bucket user ID
                jedis.sadd("user:sset", getId());
            }
        });
    }
    
    @Override
    public void uncache(final CacheContext<Void> context) {
    
    }
}

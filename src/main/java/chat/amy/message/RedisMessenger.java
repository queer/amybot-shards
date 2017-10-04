package chat.amy.message;

import chat.amy.AmybotShard;
import chat.amy.discord.RawEvent;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author amy
 * @since 9/22/17.
 */
@SuppressWarnings("FieldCanBeLocal")
public class RedisMessenger implements EventMessenger {
    private final AmybotShard shard;
    
    public RedisMessenger(final AmybotShard shard) {
        this.shard = shard;
    }
    
    private void cache(final Consumer<Jedis> op) {
        try(Jedis jedis = shard.getRedis().getResource()) {
            jedis.auth(System.getenv("REDIS_PASS"));
            op.accept(jedis);
        }
    }
    
    @Override
    public void queue(final RawEvent rawEvent) {
        // Append raw event JSON to the end of the queue
        cache(jedis -> jedis.rpush("discord-intake", rawEvent.getRaw()));
    }
    
    @Override
    public Optional<RawEvent> poll() {
        throw new UnsupportedOperationException("Shards should not be polling!");
    }
}

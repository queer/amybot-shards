package chat.amy.shard;

/**
 * @author amy
 * @since 10/1/17.
 */
public interface Sharder {
    int getShardId();
    int getShardScale();
}

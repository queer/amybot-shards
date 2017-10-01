package chat.amy.shard;

/**
 * @author amy
 * @since 10/1/17.
 */
public class EnvSharder implements Sharder {
    @Override
    public int getShardId() {
        return Integer.parseInt(System.getenv("SHARD_ID"));
    }
    
    @Override
    public int getShardScale() {
        return Integer.parseInt(System.getenv("SHARD_SCALE"));
    }
}

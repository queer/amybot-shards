package chat.amy.discord;

/**
 * @author amy
 * @since 10/3/17.
 */
public interface WSEventHandler<V> {
    void handle(WSEventContext<V> ctx);
}

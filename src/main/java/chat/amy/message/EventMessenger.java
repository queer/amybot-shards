package chat.amy.message;

import chat.amy.jda.WrappedEvent;

import java.util.Optional;

/**
 * Event message queue thing.
 *
 * @author amy
 * @since 9/22/17.
 */
public interface EventMessenger {
    void queue(WrappedEvent event);
    
    Optional<WrappedEvent> poll();
}

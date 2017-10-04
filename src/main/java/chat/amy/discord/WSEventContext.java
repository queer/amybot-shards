package chat.amy.discord;

import chat.amy.AmybotShard;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 10/3/17.
 */
public class WSEventContext<E> {
    @Getter
    private final Logger logger = LoggerFactory.getLogger("" + this);
    @Getter
    private final WSEventManager wsEventManager;
    @Getter
    private final RawEvent event;
    @Getter
    private final E data;
    
    public WSEventContext(final WSEventManager wsEventManager, final RawEvent event) {
        this(wsEventManager, event, null);
    }
    
    public WSEventContext(final WSEventManager wsEventManager, final RawEvent event, final E data) {
        this.event = event;
        this.data = data;
        this.wsEventManager = wsEventManager;
    }
    
    public AmybotShard getShard() {
        return wsEventManager.getShard();
    }
}

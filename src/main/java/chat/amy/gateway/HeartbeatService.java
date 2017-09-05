package chat.amy.gateway;

import com.google.common.util.concurrent.AbstractScheduledService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author amy
 * @since 9/4/17.
 */
public class HeartbeatService extends AbstractScheduledService {
    private final GatewayConnection gatewayConnection;
    private AtomicBoolean canSend = new AtomicBoolean(false);
    
    public HeartbeatService(final GatewayConnection gatewayConnection) {
        this.gatewayConnection = gatewayConnection;
    }
    
    @Override
    protected void runOneIteration() throws Exception {
        if(canSend.get()) {
            gatewayConnection.queue(new GatewayMessage(1, null));
        }
    }
    
    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0L, 250L, TimeUnit.MILLISECONDS);
    }
    
    @Override
    protected void startUp() throws Exception {
        canSend.set(true);
    }
    
    @Override
    protected void shutDown() throws Exception {
        canSend.set(false);
    }
}

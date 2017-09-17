package chat.amy;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

/**
 * @author amy
 * @since 9/17/17.
 */
public class ShardQueueConsumer extends DefaultConsumer {
    private final AmybotShard shard;
    
    public ShardQueueConsumer(final AmybotShard shard, final Channel channel) {
        super(channel);
        this.shard = shard;
    }
    
    @Override
    public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
        // TODO: Actually queue this up somewhere and do something with it
        // This means actually coming up with an event format... Should just convert to JSON?
        final String message = new String(body, "UTF-8");
        shard.getLogger().info(" [x] Received '" + message + '\'');
    }
}

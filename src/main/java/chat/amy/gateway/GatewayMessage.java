package chat.amy.gateway;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author amy
 * @since 9/4/17.
 */
@Value
@RequiredArgsConstructor
public class GatewayMessage {
    private final int op;
    private final Object data;
}

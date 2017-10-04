package chat.amy.discord.handle;

import chat.amy.discord.WSEventContext;
import chat.amy.discord.WSEventHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author amy
 * @since 10/3/17.
 */
public class ReadyHandler implements WSEventHandler<Void> {
    @Override
    public void handle(final WSEventContext<Void> ctx) {
        final JSONArray guilds = ctx.getEvent().getData().getJSONArray("guilds");
        ctx.getWsEventManager().getStreamableGuilds().addAll(StreamSupport.stream(guilds.spliterator(), false)
                .map(JSONObject.class::cast).map(o -> o.getString("id")).collect(Collectors.toList()));
        ctx.getWsEventManager().setStart(System.currentTimeMillis());
        ctx.getLogger().info("Connected and ready to stream guilds");
    }
}

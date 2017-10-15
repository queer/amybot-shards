package chat.amy.discord.handle.guild.channel;

import chat.amy.cache.JsonCached;
import chat.amy.cache.guild.Channel;
import chat.amy.discord.CachedEventContext;
import chat.amy.discord.CachedEventHandler;

/**
 * @author amy
 * @since 10/14/17.
 */
public class ChannelUpdateHandler implements CachedEventHandler, JsonCached {
    @Override
    public void handle(final CachedEventContext ctx) {
        final Channel channel = readJsonEvent(ctx.getEvent(), Channel.class);
        ctx.getShard().getCacheController().getMapper(channel).map(channel);
    }
}
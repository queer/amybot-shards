package chat.amy.discord.handle.guild;

import chat.amy.cache.JsonCached;
import chat.amy.cache.raw.RawGuild;
import chat.amy.discord.CachedEventContext;
import chat.amy.discord.CachedEventHandler;

/**
 * @author amy
 * @since 10/3/17.
 */
public class GuildDeleteHandler implements CachedEventHandler, JsonCached {
    @Override
    public void handle(final CachedEventContext ctx) {
        ctx.cache(jedis -> {
            final RawGuild rawGuild = readJsonEvent(ctx.getEvent(), RawGuild.class);
            /*
            final Guild guild = CachedObject.cacheRead(CacheReadContext.fromContext(new CacheContext<>(ctx.getData()), "guild:" + rawGuild.getId() + ":bucket", Guild.class));
            rawGuild.uncache(new CacheContext<>(ctx.getData()));
            guild.uncache(new CacheContext<>(ctx.getData()));
            */
        });
    }
}

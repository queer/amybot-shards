package chat.amy.discord.handle;

import chat.amy.cache.JsonCached;
import chat.amy.cache.context.CacheContext;
import chat.amy.cache.guild.Guild;
import chat.amy.cache.raw.RawGuild;
import chat.amy.discord.CachedEventContext;
import chat.amy.discord.CachedEventHandler;

/**
 * @author amy
 * @since 10/3/17.
 */
public class GuildCreateHandler implements CachedEventHandler, JsonCached {
    @Override
    public void handle(final CachedEventContext ctx) {
        final RawGuild rawGuild = readJsonEvent(ctx.getEvent(), RawGuild.class);
        final Guild guild = Guild.fromRaw(rawGuild);
        
        if(ctx.getWsEventManager().isStreamingGuilds()) {
            if(!ctx.getWsEventManager().getStreamableGuilds().contains(rawGuild.getId())) {
                // If we're streaming guilds, and it's not a preload guild, cache it to be replayed later
                ctx.getWsEventManager().getPreloadEventCache().add(ctx.getEvent());
                return;
            }
        }
        
        // This seems Fast Enough:tm: for now; prod. shard boots about 3 seconds after finishing the login,
        // which is probably good enough for this.
        //noinspection CodeBlock2Expr
        ctx.getWsEventManager().getPool().execute(() -> {
            // Bucket the guild itself
            guild.cache(new CacheContext<>(ctx.getData()));
            // Bucket channels, members, roles, ...
            rawGuild.cache(new CacheContext<>(ctx.getData()));
        });
        
        // If we're streaming guilds, we need to make sure we mark "finished" when we're done
        if(ctx.getWsEventManager().isStreamingGuilds()) {
            if(ctx.getWsEventManager().getStreamableGuilds().contains(rawGuild.getId())) {
                ctx.getWsEventManager().incrementStreamedGuilds();
                if(ctx.getWsEventManager().getStreamedGuildCount() == ctx.getWsEventManager().getStreamableGuilds().size()) {
                    ctx.getWsEventManager().setStreamingGuilds(false);
                    final long end = System.currentTimeMillis();
                    ctx.cache(jedis -> {
                        ctx.getLogger().info("Started up in " + (end - ctx.getWsEventManager().getStart()) + "ms");
                        ctx.getLogger().info("Our caches vs JDA:");
                        ctx.getLogger().info("Guilds:  {} vs {}", jedis.scard("guild:sset"), ctx.getShard().getJda().getGuildCache().size());
                        ctx.getLogger().info("Users:   {} vs {}", jedis.scard("user:sset"), ctx.getShard().getJda().getUserCache().size());
                    });
                    ctx.getWsEventManager().getPreloadEventCache().forEach(ctx.getShard().getMessenger()::queue);
                }
            }
        }
    }
}

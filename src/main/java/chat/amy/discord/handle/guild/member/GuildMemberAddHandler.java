package chat.amy.discord.handle.guild.member;

import chat.amy.cache.JsonCached;
import chat.amy.cache.guild.Guild;
import chat.amy.cache.guild.Member;
import chat.amy.discord.CachedEventContext;
import chat.amy.discord.CachedEventHandler;

/**
 * @author amy
 * @since 10/14/17.
 */
public class GuildMemberAddHandler implements CachedEventHandler, JsonCached {
    @Override
    public void handle(final CachedEventContext ctx) {
        final Guild guild = ctx.getShard().getCacheController().getMapper(Guild.class)
                .unmap(ctx.getEvent().getData().getJSONObject("d").getString("guild_id"), Guild.class);
        guild.getMembers().add(readJsonEvent(ctx.getEvent(), Member.class));
        ctx.getShard().getCacheController().getMapper(guild).map(guild);
    }
}
package chat.amy.discord.handle.guild.member;

import chat.amy.cache.JsonCached;
import chat.amy.cache.guild.Guild;
import chat.amy.cache.guild.Member;
import chat.amy.cache.user.User;
import chat.amy.discord.CachedEventContext;
import chat.amy.discord.CachedEventHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author amy
 * @since 10/14/17.
 */
public class GuildMemberUpdateHandler implements CachedEventHandler, JsonCached {
    @Override
    public void handle(final CachedEventContext ctx) {
        // Get the guild
        final Guild guild = ctx.getShard().getCacheController().getMapper(Guild.class)
                .unmap(ctx.getEvent().getData().getJSONObject("d").getString("guild_id"), Guild.class);
        // Get the roles
        final List<String> roleIds = new ArrayList<>();
        final Iterator<Object> i = ctx.getEvent().getData().getJSONObject("d").getJSONArray("roles").iterator();
        while(i.hasNext()) {
            roleIds.add((String) i.next());
        }
        String nick = ctx.getEvent().getData().getString("nick");
        final User user = readJson(ctx.getEvent().getData().getJSONObject("user").toString(), User.class);
        // Filter out the member in question
        final Optional<Member> first = guild.getMembers().stream().filter(e -> e.getId().equals(user.getId())).findFirst();
        if(first.isPresent()) {
            final Member member = first.get();
            // Is this stupid?
            guild.getMembers().remove(member);
            member.setNickname(nick);
            member.setId(user.getId());
            member.getRoles().clear();
            member.getRoles().addAll(roleIds);
            guild.getMembers().add(member);
            // Update both the user and the guild in the cache
            ctx.getShard().getCacheController().getMapper(user).map(user);
            ctx.getShard().getCacheController().getMapper(guild).map(guild);
        }
    }
}
package chat.amy.discord.handle;

import chat.amy.cache.CachedObject;
import chat.amy.cache.JsonCached;
import chat.amy.cache.context.CacheContext;
import chat.amy.cache.context.CacheReadContext;
import chat.amy.cache.guild.Guild;
import chat.amy.cache.guild.Member;
import chat.amy.cache.raw.RawMember;
import chat.amy.discord.CachedEventContext;
import chat.amy.discord.CachedEventHandler;
import org.json.JSONObject;

import java.util.stream.StreamSupport;

/**
 * @author amy
 * @since 10/3/17.
 */
public class GuildMembersChunkHandler implements CachedEventHandler, JsonCached {
    @Override
    public void handle(final CachedEventContext ctx) {
        // This event is basically just
        // {
        //   "guild_id": "12345678901234567",
        //   "members": [
        //     ...
        //   ]
        // }
        //
        // So we deserialize the members array, and add them to
        // - The guild in question
        // - The global set of users, as needed
        // Then re-cache the guild to make sure it's up-to-date
        final JSONObject data = ctx.getEvent().getData().getJSONObject("d");
        final String guildId = data.getString("guild_id");
        final Guild guild = CachedObject.cacheRead(new CacheReadContext<>(ctx.getData(), "guild:" + guildId + ":bucket", Guild.class));
        final CacheContext<String> context = new CacheContext<>(ctx.getData(), guildId);
        StreamSupport.stream(data.getJSONArray("members").spliterator(), false).map(JSONObject.class::cast)
                .forEach(e -> {
                    final RawMember rawMember = readJson(e.toString(), RawMember.class);
                    guild.getMembers().add(Member.fromRaw(rawMember));
                    rawMember.cache(context);
                });
        guild.cache(new CacheContext<>(ctx.getData(), null));
    }
}

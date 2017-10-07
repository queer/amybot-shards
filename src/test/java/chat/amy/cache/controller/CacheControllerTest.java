package chat.amy.cache.controller;

import chat.amy.cache.guild.Guild;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author amy
 * @since 10/7/17.
 */
public class CacheControllerTest {
    private final CacheController cacheController = new CacheControllerImpl(null);
    
    @Test
    public void testCacheGuild() {
        long start = System.currentTimeMillis();
        final CacheMapper guildMapper = cacheController.getMapper(Guild.class);
        // wew
        final Guild guild = new Guild(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "12345",
                "test", "", "", "123456", "UNKNOWN", "", 0,
                false, null, 0, 0, 0,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, "",
                false, "", "", false, false, 0,
                Collections.emptyList());
        guildMapper.map(guild);
        Guild mapped = guildMapper.unmap("12345", Guild.class);
        assertEquals(guild, mapped);
    }
    
    @Test
    public void testCacheChannel() {
    
    }
    
    @Test
    public void testCacheMember() {
    
    }
    
    @Test
    public void testCacheEmote() {
    
    }
    
    @Test
    public void testCacheRole() {
    
    }
    
    @Test
    public void testCacheUser() {
    
    }
}
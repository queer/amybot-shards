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
        System.out.println("Guild create: " + (System.currentTimeMillis() - start) + "ms");
        guildMapper.map(guild);
        System.out.println("Mapped in: " + (System.currentTimeMillis() - start) + "ms");
        Guild mapped = guildMapper.unmap("12345", Guild.class);
        System.out.println("Mapped out: " + (System.currentTimeMillis() - start) + "ms");
        assertEquals(guild, mapped);
        System.out.println("Full guild cache: " + (System.currentTimeMillis() - start) + "ms");
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
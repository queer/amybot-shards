package chat.amy.cache.controller;

import chat.amy.cache.Snowflake;

/**
 * Control how objects get mapped back and forth in the cache. The idea of this
 * class is that objects can be transparently mapped to a variety of backends -
 * postgres, redis, mongo, rethonk, ... - without the user actually having to
 * know or care about the specifics thereof.
 *
 * @author amy
 * @since 10/4/17.
 */
public interface CacheController {
    /**
     * Set the defaults, and register any specific mappers that we want
     *
     * TODO: Should be able to configure mapper types through env. vars., ex so that I can keep storing emotes in postgres transparently
     */
    void setup();
    
    /**
     * Get the mapper for a given object, so that it can just be used directly
     * instead of adding ANOTHER layer of abstraction that really isn't needed.
     *
     * @param object
     * @param <E>
     *
     * @return
     */
    default <E extends Snowflake> CacheMapper getMapper(E object) {
        return getMapper(object.getClass());
    }
    
    <E extends Snowflake> CacheMapper getMapper(Class<E> object);
}

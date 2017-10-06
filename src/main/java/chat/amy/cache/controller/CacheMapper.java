package chat.amy.cache.controller;

import chat.amy.cache.Snowflake;

import java.util.Collection;

/**
 * @author amy
 * @since 10/4/17.
 */
public interface CacheMapper {
    /**
     * Map some object transparently to the backend storage system, based on snowflake or whatever.
     *
     * @param object Object to map
     * @param <E>    Type of object
     */
    <E extends Snowflake> void map(E object);
    
    /**
     * A nicer way of {@link #map}ing a lot of snowflakes at once
     *
     * @param objects
     * @param <E>
     */
    <E extends Snowflake> void batchMap(Collection<E> objects);
    
    /**
     * Unmap some object with the given snowflake and type transparently
     *
     * @param snowflake Snowflake to unmap
     * @param clz       Object class so we can deserialize it properly
     * @param <E>       Type of object
     *
     * @return
     */
    <E extends Snowflake> E unmap(String snowflake, Class<E> clz);
}

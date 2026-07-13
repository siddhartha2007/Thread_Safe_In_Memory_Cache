package cache;
/**
 * A generic in-memory cache abstraction for storing key-value pairs.
 *
 * <p>The cache supports basic CRUD operations, optional time-to-live (TTL)
 * expiration, and implementations may apply different eviction policies
 * when the configured capacity is reached.
 *
 * <p>This interface defines the contract for cache implementations and
 * does not specify how entries are stored or evicted.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 */
public interface Cache<K,V>{

    /**
     * Stores the specified value in the cache.
     *
     * <p>If the key already exists, its value is replaced.
     *
     * @param key the key used to identify the cached value
     * @param value the value to be stored
     */
    void put(K key, V value);

    /**
     * Stores the specified value in the cache with a time-to-live (TTL).
     *
     * <p>If the key already exists, its value is replaced and the TTL
     * is reset.
     *
     * @param key the key used to identify the cached value
     * @param value the value to be stored
     * @param ttlMillis the entry lifetime in milliseconds
     */
    void put(K key, V value, long ttlMillis);

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the cached value if present and not expired;
     *         otherwise {@code null}
     */
    V get(K key);


    /**
     * Removes the mapping for the specified key if it exists.
     *
     * @param key the key to remove
     */
    void remove(K key);

    /**
     * Determines whether a non-expired entry exists for the specified key.
     *
     * @param key the key to check
     * @return {@code true} if the key exists and has not expired;
     *         {@code false} otherwise
     */
    boolean containsKey(K key);

    /**
     * Returns the number of entries currently stored in the cache.
     *
     * @return the current cache size
     */
    int size();

    /**
     * Removes all entries from the cache.
     */
    void clear();

}

// we are using generics so that the cache is reusable for any application.

// for now the put() method is returning void.
// later we can return V so that it returns the old value associated with the key .


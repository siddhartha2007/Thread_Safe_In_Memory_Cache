package eviction;

/**
 * Defines the contract for cache eviction strategies.
 *
 * <p>An eviction policy tracks cache access patterns and determines
 * which key should be removed when the cache reaches its capacity.
 *
 * <p>Implementations are responsible for maintaining any internal
 * state required to make eviction decisions.
 *
 * @param <K> the type of cache keys managed by this eviction policy
 */
public interface EvictionPolicy<K> {

    /**
     * Notifies the eviction policy that a new key has been inserted.
     *
     * @param key the inserted key
     */
    // called whenever a new key is inserted into cache.
    void  onInsert(K key);

    /**
     * Notifies the eviction policy that a key has been accessed.
     *
     * @param key the accessed key
     */
    // called Whenever an existing key is successfully accessed.
    void onAccess(K key);

    /**
     * Notifies the eviction policy that a key has been removed
     * from the cache.
     *
     * @param key the removed key
     */
    // called when a key is removed from cache dure to expiration or explicit removal
    void onRemove(K key);

    /**
     * Selects and removes the next key to evict according to
     * the eviction strategy.
     *
     * @return the key selected for eviction, or {@code null}
     * if no key is available
     */
    K evict();

    /**
     * Removes all internal state maintained by the eviction policy.
     */
    void clear();
}

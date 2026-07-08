package eviction;

public interface EvictionPolicy<K> {

    // called whenever a new key is inserted into cache.
    void  onInsert(K key);

    // called Whenever an existing key is successfully accessed.
    void onAccess(K key);

    // called when a key is removed from cache dure to expiration or explicit removal
    void onRemove(K key);

    K evict();

    void clear();
}

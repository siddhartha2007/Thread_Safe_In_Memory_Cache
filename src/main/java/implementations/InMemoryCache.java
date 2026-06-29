package implementations;

import cache.Cache;
import model.CacheEntry;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCache<K,V> implements Cache<K,V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;

    public InMemoryCache(){
        this.cache=new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value));
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        cache.put(key, new CacheEntry<>(value,ttlMillis));
    }

    @Override
    public V get(K key) {
        CacheEntry<V> entry=cache.get(key);
        if(entry==null){
            return null;// returning null is industry standard
        }
        if(entry.isExpired()){
            this.remove(key);
            return null;
        }
        entry.incrementAccessCount();
        entry.updateLastAccessTime();
        return entry.getValue();
    }

    @Override
    public void remove(K key) {

        CacheEntry<V> entry=cache.get(key);
        if(entry==null) {
            return;
        }

        // future metrics logic.
        cache.remove(key);
    }

    @Override
    public boolean containsKey(K key) {
        CacheEntry<V> entry=cache.get(key);
        if(entry==null){
            return false;
        }

        if(entry.isExpired()) {
            this.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}

package implementations;

import cache.Cache;
import metrics.CacheMetrics;
import model.CacheEntry;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCache<K,V> implements Cache<K,V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final CacheMetrics metrics;

    public InMemoryCache(){
        this.cache=new ConcurrentHashMap<>();
        this.metrics=new CacheMetrics();
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
            metrics.incrementMisses();
            return null;// returning null is industry standard
        }
        if(entry.isExpired()){
            removeExpiredEntry(key);
            metrics.incrementMisses();
            return null;
        }
        metrics.incrementHits();
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
            removeExpiredEntry(key);
            return false;
        }
        return true;
    }

    @Override
    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

    private boolean removeExpiredEntry(K key){
        boolean removed= cache.remove(key) !=null;
        if(removed){
            this.metrics.incrementExpiredEntries();
        }
        return removed;
    }

    }

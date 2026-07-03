package implementations;

import cache.Cache;
import metrics.CacheMetrics;
import model.CacheEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InMemoryCache<K,V> implements Cache<K,V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> Cache;
    private final CacheMetrics metrics;
    private final ScheduledExecutorService scheduler;
    private final long cleanupIntervalMillis;
    public InMemoryCache() {
        this(20000);
    }

    public InMemoryCache(long cleanupIntervalMillis){
        Cache =new ConcurrentHashMap<>();
        metrics=new CacheMetrics();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupIntervalMillis=cleanupIntervalMillis;
        Runnable cleanUpTask=() ->{
            removeExpiredEntries();
        };
        scheduler.scheduleWithFixedDelay(cleanUpTask,cleanupIntervalMillis, cleanupIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(K key, V value) {
        Cache.put(key, new CacheEntry<>(value));
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        Cache.put(key, new CacheEntry<>(value,ttlMillis));
    }

    @Override
    public V get(K key) {
        CacheEntry<V> entry= Cache.get(key);
        if(entry==null){
            metrics.incrementMisses();
            return null;// returning null is industry standard
        }
        if(entry.isExpired()){
            removeExpiredEntry(key,entry);
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

        CacheEntry<V> removedEntry= Cache.remove(key);
        if(removedEntry!=null) {
            // future metrics logic
        }
    }

    @Override
    public boolean containsKey(K key) {
        CacheEntry<V> entry= Cache.get(key);
        if(entry==null){
            return false;
        }

        if(entry.isExpired()) {
            removeExpiredEntry(key,entry);
            return false;
        }
        return true;
    }

    @Override
    public int size() {
        return Cache.size();
        // this is not accurate. we are removing entries lazily. there are high chances that the expired entries still exists in our cache map.
    }

    public void clear() {
        Cache.clear();
    }

    private boolean removeExpiredEntry(K key,CacheEntry<V> value){
        boolean removed= Cache.remove(key,value);
        if(removed){
            this.metrics.incrementExpiredEntries();
        }
        return removed;
    }

    public void shutdown(){
        scheduler.shutdown();
        try{
            if(!scheduler.awaitTermination(10,TimeUnit.SECONDS)){
                scheduler.shutdownNow();
            }
        }catch (InterruptedException e){
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public void removeExpiredEntries(){
        try{
            for(Map.Entry<K,CacheEntry<V>> cache: Cache.entrySet()){
                CacheEntry<V> cacheEntry= cache.getValue();
                if(cacheEntry.isExpired()){
                    removeExpiredEntry(cache.getKey(),cacheEntry);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            // logging will be implemented in the future
        }
    }
}

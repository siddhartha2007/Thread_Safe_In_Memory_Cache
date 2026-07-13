package implementation;

import cache.Cache;
import eviction.EvictionPolicy;
import metrics.CacheMetrics;
import model.CacheEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryCache<K,V> implements Cache<K,V> {
    private final Map<K, CacheEntry<V>> cache;
    private final CacheMetrics metrics;
    private final ScheduledExecutorService scheduler;
    private final long cleanupIntervalMillis;
    private final int capacity;
    private final EvictionPolicy<K> evictionPolicy;
    private final ReentrantLock cacheLock = new ReentrantLock();

    private static final long DEFAULT_CLEANUP_INTERVAL=20_000;
    private static final int DEFAULT_CAPACITY=100;
    public InMemoryCache(EvictionPolicy<K> evictionPolicy){
        this(DEFAULT_CLEANUP_INTERVAL,DEFAULT_CAPACITY,evictionPolicy);
    }


    public InMemoryCache(long cleanupIntervalMillis,EvictionPolicy<K> evictionPolicy) {
        this(cleanupIntervalMillis,DEFAULT_CAPACITY,evictionPolicy);
        if(cleanupIntervalMillis<=0){
            throw new IllegalArgumentException();
        }
    }

    public InMemoryCache(int capacity,EvictionPolicy<K> evictionPolicy) {
        this(DEFAULT_CLEANUP_INTERVAL,capacity,evictionPolicy);
        if(capacity<=0){
            throw new IllegalArgumentException();
        }
    }

    public InMemoryCache(long cleanupIntervalMillis, int capacity,EvictionPolicy<K> evictionPolicy){
        if(cleanupIntervalMillis<=0 || capacity<=0){
            throw new IllegalArgumentException();
        }

        cache =new ConcurrentHashMap<>();
        metrics=new CacheMetrics();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupIntervalMillis=cleanupIntervalMillis;
        this.capacity=capacity;
        this.evictionPolicy=evictionPolicy;
        Runnable cleanUpTask=() ->{
            removeExpiredEntries();
        };
        scheduler.scheduleWithFixedDelay(cleanUpTask,cleanupIntervalMillis, cleanupIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(K key, V value) {

        cacheLock.lock();
        try {

            CacheEntry<V> existing = cache.get(key);
            if(existing!=null){
                evictionPolicy.onAccess(key);
            }else {
                if (cache.size() >= capacity) {
                    K victim = evictionPolicy.evict();
                    if (victim != null) {
                        remove(victim);
                        metrics.incrementEvictions();
                    }
                }
                evictionPolicy.onInsert(key);
            }
            cache.put(key, new CacheEntry<>(value));
        } finally {
            cacheLock.unlock();
        }

    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        cacheLock.lock();
        try {

            CacheEntry<V> existing = cache.get(key);
            if(existing!=null){
                evictionPolicy.onAccess(key);
            }else {
                if (cache.size() >= capacity) {
                    K victim = evictionPolicy.evict();
                    if (victim != null) {
                        remove(victim);
                        metrics.incrementEvictions();
                    }
                }
                evictionPolicy.onInsert(key);
            }
            cache.put(key, new CacheEntry<>(value,ttlMillis));
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public V get(K key) {
        CacheEntry<V> entry= cache.get(key);
        if(entry==null){
            metrics.incrementMisses();
            return null;// returning null is industry standard
        }
        if(entry.isExpired()){
            removeExpiredEntry(key,entry);
            metrics.incrementMisses();
            return null;
        }
        evictionPolicy.onAccess(key);
        metrics.incrementHits();
        entry.incrementAccessCount();
        entry.updateLastAccessTime();
        return entry.getValue();
    }

    @Override
    public void remove(K key) {

        cacheLock.lock();
        try {
            CacheEntry<V> removedEntry = cache.remove(key);
            if (removedEntry != null) {
                // future metrics logic
                evictionPolicy.onRemove(key);
            }
        }finally {
            cacheLock.unlock();
        }
    }

    @Override
    public boolean containsKey(K key) {
        CacheEntry<V> entry= cache.get(key);
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
        return cache.size();
    }

    public void clear() {
        cacheLock.lock();
        try {
            evictionPolicy.clear();
            cache.clear();
        }finally {
            cacheLock.unlock();
        }
    }

    private boolean removeExpiredEntry(K key,CacheEntry<V> value){
        boolean removed =false;
        cacheLock.lock();
        try {
            removed = cache.remove(key, value);
            if (removed) {
                evictionPolicy.onRemove(key);
                this.metrics.incrementExpiredEntries();
            }
            return removed;
        } finally {
            cacheLock.unlock();
        }
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


    private void removeExpiredEntries(){
        try{
            for(Map.Entry<K,CacheEntry<V>> cache: cache.entrySet()){
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

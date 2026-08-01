package model;

import exceptions.InvalidTtlException;

import java.util.concurrent.atomic.LongAdder;
/**
 * Represents a single cache entry stored in the cache.
 *
 * <p>Each entry stores the cached value along with metadata used for
 * expiration and access statistics.
 *
 * @param <V> the type of the cached value
 */
public class CacheEntry<V>{
    private final long expiryTime;

    private final V value;

    private final LongAdder accessCount;

    private volatile  long lastAccessTime;

    public CacheEntry(V value,long ttlMillis){
        if(ttlMillis<=0){
            throw new InvalidTtlException("TtlMillis cannot be Negative or Zero");
        }
        this.value=value;
        this.expiryTime=System.currentTimeMillis()+ttlMillis;
        this.accessCount=new LongAdder();
        this.lastAccessTime=System.currentTimeMillis();
    }

    public CacheEntry(V value){
        this.value=value;
        this.expiryTime=Long.MAX_VALUE;
        this.accessCount=new LongAdder();
        this.lastAccessTime=System.currentTimeMillis();
    }


    /**
     * Determines whether this cache entry has expired.
     *
     * @return {@code true} if the entry has expired; {@code false} otherwise
     */
    public boolean isExpired(){
        return System.currentTimeMillis()>expiryTime;
    }

    public void updateLastAccessTime(){
        this.lastAccessTime=System.currentTimeMillis();
    }

    public V getValue() {
        return this.value;
    }

    public void incrementAccessCount(){
        accessCount.increment();
    }

    public long getAccessCount(){
        return accessCount.sum();
    }

    public long getLastAccessTime(){
        return this.lastAccessTime;
    }

}

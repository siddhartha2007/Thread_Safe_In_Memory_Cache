package model;

import java.util.concurrent.atomic.LongAdder;

public class CacheEntry<V>{
    private final long expiryTime;

    private final V value;

    private final LongAdder accessCount;

    private volatile  long lastAccessTime;

    public CacheEntry(V value,long ttlMillis){
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

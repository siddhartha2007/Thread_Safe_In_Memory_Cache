package metrics;

import java.util.concurrent.atomic.LongAdder;
/**
 * Collects runtime statistics about cache operations.
 *
 * <p>The metrics include cache hits, cache misses, evictions,
 * and expired entry removals.
 *
 * <p>This class is thread-safe.
 */
public class CacheMetrics {
    private final LongAdder cacheHits;
    private final LongAdder cacheMisses;
    private final LongAdder cacheEvictions;
    private final LongAdder expiredEntries;

    public CacheMetrics() {
        this.cacheHits = new LongAdder();
        this.cacheMisses = new LongAdder();
        this.cacheEvictions = new LongAdder();
        this.expiredEntries = new LongAdder();
    }

    /**
     * Increments the cache hit count.
     */
    public void incrementHits(){
        this.cacheHits.increment();
    }

    public void incrementMisses() {
        this.cacheMisses.increment();
    }

    public void incrementEvictions(){
        this.cacheEvictions.increment();
    }

    public  void incrementExpiredEntries(){
        this.expiredEntries.increment();
    }

    // getters
    /**
     * Returns the total number of cache hits.
     */
    public long getCacheHits(){
        return this.cacheHits.sum();
    }


    public long getCacheMisses(){
        return this.cacheMisses.sum();
    }

    public long getCacheEvictions(){
        return this.cacheEvictions.sum();
    }

    public long getExpiredEntries(){
        return this.expiredEntries.sum();
    }

    @Override
    public String toString(){
        return "Cache Metrics : cacheHits = "+getCacheHits()+" , cacheMisses = "+getCacheMisses()+" , cacheEvitions = "+getCacheEvictions()+" , expiredEntries = "+getExpiredEntries();
    }

}

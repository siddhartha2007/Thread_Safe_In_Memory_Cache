package implementation;

import scheduler.CleanUpScheduler;
import cache.Cache;
import eviction.EvictionPolicy;
import metrics.CacheMetrics;
import metrics.CacheStats;
import model.CacheEntry;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe in-memory implementation of the {@link Cache} interface.
 *
 * <p>This implementation stores entries in a {@code ConcurrentHashMap}
 * and supports:
 * <ul>
 *     <li>Time-to-live (TTL) expiration</li>
 *     <li>Capacity-based eviction</li>
 *     <li>Pluggable eviction policies</li>
 *     <li>Background cleanup of expired entries</li>
 * </ul>
 *
 * <p>Cache operations are coordinated using a {@code ReentrantLock}
 * to ensure consistency between the cache storage and the configured
 * eviction policy.
 *
 * @param <K> the type of cache keys
 * @param <V> the type of cached values
 */
public class InMemoryCache<K,V> implements Cache<K,V> {
    private final Map<K, CacheEntry<V>> cache;
    private final CacheMetrics metrics;
    private final ScheduledFuture<?> cleanupFuture;
    private final long cleanupIntervalMillis;
    private final int capacity;
    private final EvictionPolicy<K> evictionPolicy;
    private final ReentrantLock cacheLock = new ReentrantLock();
    private final AtomicBoolean shutdown =new AtomicBoolean(false);

    private static final long DEFAULT_CLEANUP_INTERVAL=20_000;
    private static final int DEFAULT_CAPACITY=100;


    public InMemoryCache(EvictionPolicy<K> evictionPolicy){
        this(DEFAULT_CLEANUP_INTERVAL,DEFAULT_CAPACITY,evictionPolicy);
    }


    public InMemoryCache(long cleanupIntervalMillis,EvictionPolicy<K> evictionPolicy) {
        this(cleanupIntervalMillis,DEFAULT_CAPACITY,evictionPolicy);
    }

    public InMemoryCache(int capacity,EvictionPolicy<K> evictionPolicy) {
        this(DEFAULT_CLEANUP_INTERVAL,capacity,evictionPolicy);
    }

    private InMemoryCache(InMemoryCacheBuilder builder){
            this(builder.cleanUpIntervalMillis,builder.capacity,builder.evictionPolicy);
    }

    /**
     * Creates a new thread-safe in-memory cache.
     *
     * <p>Each cache instance maintains its own storage, metrics, and
     * eviction policy while sharing a common background cleanup scheduler
     * with other cache instances. A periodic cleanup task is automatically
     * registered with the shared scheduler to remove expired entries.
     *
     * @param cleanupIntervalMillis interval, in milliseconds, between
     *                              successive background cleanup executions
     * @param capacity maximum number of entries the cache can hold
     * @param evictionPolicy eviction strategy used when the cache reaches
     *                       its capacity
     * @throws IllegalArgumentException if the eviction policy is {@code null},
     *                                  the cleanup interval is non-positive,
     *                                  or the capacity is non-positive
     */
    public InMemoryCache(long cleanupIntervalMillis, int capacity,EvictionPolicy<K> evictionPolicy){
        if(evictionPolicy==null){
            throw new IllegalArgumentException("Eviction Policy Cannot be Null");
        }

        if(capacity<=0){
            throw new IllegalArgumentException("Capacity Cannot be Non Positive");
        }
        if(cleanupIntervalMillis<=0){
            throw new IllegalArgumentException("Clean Up Interval Cannot be Non Positive");
        }
        cache =new ConcurrentHashMap<>();
        metrics=new CacheMetrics();
        this.cleanupIntervalMillis=cleanupIntervalMillis;
        this.capacity=capacity;
        this.evictionPolicy=evictionPolicy;
        Runnable task= ()->{
            removeExpiredEntries();
        };
        cleanupFuture= CleanUpScheduler.getInstance().scheduleForCleanUp(task,cleanupIntervalMillis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        ensureOpen();
        cacheLock.lock();
        try {
            CacheEntry<V> existing = cache.get(key);
            if (existing != null) {
                evictionPolicy.onAccess(key);
            } else {
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
    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value, long ttlMillis) {
        ensureOpen();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(K key) {
        ensureOpen();
        CacheEntry<V> entry= cache.get(key);
        if(entry==null){
            metrics.incrementMisses();
            return null;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(K key) {
        ensureOpen();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(K key) {
        ensureOpen();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        ensureOpen();
        return cache.size();
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        ensureOpen();
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

    /**
     * Stops the background cleanup task associated with this cache.
     *
     * <p>Only this cache's scheduled cleanup task is cancelled. Other
     * cache instances sharing the same scheduler continue executing their
     * cleanup tasks normally.
     *
     * <p>If a cleanup task is already executing, it is allowed to complete,
     * but no subsequent executions are scheduled.
     */
    public void stopCleanupScheduler(){
        cleanupFuture.cancel(false);
    }

    /**
     * Shuts down this cache instance.
     *
     * <p>After shutdown, all cache operations throw an
     * {@link IllegalStateException}. The background cleanup task associated
     * with this cache is cancelled, while the shared cleanup scheduler
     * remains available for other cache instances.
     *
     * <p>This method does not shut down the shared scheduler.
     * To terminate the shared scheduler, invoke
     *{@link CleanUpScheduler#shutdown()}
     */
    public void shutdown(){
        shutdown.set(true);
        stopCleanupScheduler();
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

    /**
     * Returns whether this cache has been shut down.
     *
     * @return {@code true} if this cache has been shut down;
     *         {@code false} otherwise
     */
    public boolean isShutDown(){
        return shutdown.get();
    }

    /**
     * Returns a snapshot of the current cache statistics.
     *
     * <p>The returned {@link CacheStats} is immutable and represents the
     * values of the cache metrics at the time this method is invoked.
     * Subsequent cache operations do not modify the returned object.
     *
     * @return a snapshot containing cache hits, misses, evictions,
     *         and expired entry removals
     */
    public CacheStats getStats(){
        return new CacheStats(metrics.getCacheHits(),metrics.getCacheMisses(),metrics.getCacheEvictions(),metrics.getExpiredEntries());
    }

    private void ensureOpen(){
        if(shutdown.get()){
            throw  new IllegalStateException("Cache has been shut down.");
        }
    }



    /**
     * A builder for constructing {@link InMemoryCache} instances.
     *
     * <p>This builder provides a flexible way to configure cache instances
     * without requiring multiple overloaded constructors. Optional
     * configuration values that are not explicitly specified are assigned
     * sensible default values during {@link #build()}.
     *
     * <p>Default configuration:
     * <ul>
     *     <li>Capacity: {@value InMemoryCache#DEFAULT_CAPACITY}</li>
     *     <li>Cleanup Interval: {@value InMemoryCache#DEFAULT_CLEANUP_INTERVAL} milliseconds</li>
     * </ul>
     *
     * <p>An {@link EvictionPolicy} must be provided before invoking
     * {@link #build()}.
     *
     * @param <K> the type of cache keys
     * @param <V> the type of cached values
     */
    public static class InMemoryCacheBuilder<K,V>{
        private EvictionPolicy<K> evictionPolicy;
        private Long cleanUpIntervalMillis;
        private Integer capacity;

        /**
         * Creates a new builder for constructing {@link InMemoryCache}
         * instances.
         *
         * @param <K> the type of cache keys
         * @param <V> the type of cached values
         * @return a new {@code InMemoryCacheBuilder}
         */
        public static <K,V> InMemoryCacheBuilder<K,V> builder() {
            return new InMemoryCacheBuilder<>();
        }

        /**
         * Sets the eviction policy used by the cache.
         *
         * @param evictionPolicy the eviction policy to use
         * @return this builder instance
         */
        public InMemoryCacheBuilder<K,V> evictionPolicy(EvictionPolicy<K> evictionPolicy){
            this.evictionPolicy=evictionPolicy;
            return this;
        }

        /**
         * Sets the maximum number of entries the cache can hold.
         *
         * <p>If this method is not invoked, the default capacity is used.
         *
         * @param capacity the maximum cache capacity
         * @return this builder instance
         */
        public InMemoryCacheBuilder<K,V> capacity(int capacity){
            this.capacity=capacity;
            return this;
        }

        /**
         * Sets the interval between successive background cleanup executions.
         *
         * <p>If this method is not invoked, the default cleanup interval is
         * used.
         *
         * @param cleanUpIntervalMillis cleanup interval in milliseconds
         * @return this builder instance
         */
        public InMemoryCacheBuilder<K,V> cleanUpIntervalMillis(Long cleanUpIntervalMillis) {
            this.cleanUpIntervalMillis = cleanUpIntervalMillis;
            return this;
        }

        /**
         * Builds and returns a new {@link InMemoryCache} instance.
         *
         * <p>Any optional configuration values that were not explicitly set are
         * replaced with their default values.
         *
         * <p>An {@link IllegalStateException} is thrown if no eviction policy
         * has been provided.
         *
         * @return a newly constructed {@code InMemoryCache}
         * @throws IllegalStateException if an eviction policy has not been
         *                               configured
         */
        public InMemoryCache<K,V> build(){
            if(this.cleanUpIntervalMillis==null){
                cleanUpIntervalMillis=DEFAULT_CLEANUP_INTERVAL;
            }
            if(this.capacity==null){
                capacity=DEFAULT_CAPACITY;
            }
            if(evictionPolicy == null) {
                throw new IllegalStateException(
                        "Eviction Policy must be specified."
                );
            }
            return new InMemoryCache<>(this);
        }
    }
}

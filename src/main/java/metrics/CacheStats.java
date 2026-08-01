package metrics;

public record CacheStats(long hits,long misses,long evictions,long expiredentries) {
}

# Thread-Safe In-Memory Cache

A lightweight, generic, thread-safe in-memory cache library written in Java.

The project was built to explore concurrent programming, cache eviction strategies, clean API design, and extensible software architecture. It provides configurable TTL expiration, pluggable eviction policies, automatic cleanup of expired entries, and runtime cache metrics.

---
## Why this project?

Caching is a fundamental technique in backend systems for improving responsiveness by avoiding repeated access to expensive resources such as databases, file systems, or external services.

Instead of relying on an existing caching library, I built this project from scratch to better understand the data structures, concurrency mechanisms, and architectural decisions involved in designing a thread-safe in-memory cache.

## Features

- Generic cache implementation (`Cache<K, V>`)
- Thread-safe operations
- Configurable Time-To-Live (TTL) for cache entries
- Automatic background cleanup of expired entries
- Capacity-based cache eviction
- Pluggable eviction policy architecture
- Least Recently Used (LRU) eviction policy
- Runtime cache metrics
- Clean and extensible API
- Fully documented using JavaDocs

---

## Architecture

```text
                    Client
                       │
        put / get / remove
                       │
                       ▼
              InMemoryCache<K,V>
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
 ConcurrentHashMap          EvictionPolicy<K>
        │                             │
        │                             ▼
        │                 LRUEvictionPolicy<K>
        │                  (HashMap + Doubly Linked List)
        │
        ▼
 CacheEntry<V>

             Scheduled Cleanup Thread
                       │
                       ▼
             Removes expired entries
```

---

## Project Structure

```
src
└── main
    └── java
        ├── cache
        │     └── Cache.java
        │
        ├── implementation
        │     └── InMemoryCache.java
        │
        ├── eviction
        │     ├── EvictionPolicy.java
        │     └── LRUEvictionPolicy.java
        │
        ├── metrics
        │     └── CacheMetrics.java
        │
        ├── model
        │     └── CacheEntry.java
        │
        └── examples
              └── CacheDemo.java
```

---

# Getting Started

## Create a Cache

```java
Cache<String, String> cache =
        new InMemoryCache<>(
                100,
                new LRUEvictionPolicy<>()
        );
```

---

## Store Values

```java
cache.put("username", "Alice");
```

---

## Store Values with TTL

```java
cache.put("session", "abc123", 30_000);
```

---

## Retrieve Values

```java
String username = cache.get("username");
```

---

## Remove Entries

```java
cache.remove("username");
```

---

## Clear Cache

```java
cache.clear();
```

---

# Thread Safety

The cache is designed to support concurrent access from multiple threads.

It achieves thread safety using:

- `ConcurrentHashMap` for concurrent storage
- `ReentrantLock` to coordinate updates between the cache and eviction policy
- synchronized LRU eviction policy operations
- Background cleanup executed using `ScheduledExecutorService`

This design keeps the cache storage and eviction policy consistent even under concurrent modifications.

---

# Cache Eviction

When the configured capacity is reached, the cache delegates eviction to the configured `EvictionPolicy`.

The default implementation included in this project is:

- Least Recently Used (LRU)

The eviction policy is fully pluggable, allowing additional strategies to be implemented without modifying the cache implementation.

Example:

```java
Cache<String, User> cache =
        new InMemoryCache<>(
                100,
                new LRUEvictionPolicy<>()
        );
```

---

# LRU Implementation

The LRU policy maintains access order using two data structures:

- HashMap
- Doubly Linked List

The hash map provides constant-time lookup of nodes.

The doubly linked list maintains entries ordered by recent access.

Whenever an entry is accessed:

1. The corresponding node is located using the hash map.
2. The node is removed from its current position.
3. The node is appended to the tail of the linked list.

The head of the linked list always represents the least recently used entry and is selected during eviction.

---

# TTL Expiration

Each cache entry may optionally have a Time-To-Live (TTL).

Expired entries are removed using two mechanisms:

### Lazy Expiration

Whenever an entry is accessed, the cache checks whether it has expired.

If expired, it is removed immediately.

### Background Cleanup

A scheduled cleanup task periodically scans the cache and removes expired entries that were never accessed again.

---

# Runtime Metrics

The cache collects runtime statistics including:

- Cache Hits
- Cache Misses
- Evictions
- Expired Entries Removed

These metrics help monitor cache behavior and are designed to be extended in future releases.

---

# Time Complexity

| Operation | Complexity |
|-----------|-----------:|
| put | O(1) |
| get | O(1) |
| remove | O(1) |
| containsKey | O(1) |
| eviction | O(1) |

---

# Design Decisions

## Why ConcurrentHashMap?

It allows concurrent reads and updates while providing significantly better scalability than synchronizing an entire hash table.

---

## Why ReentrantLock?

Although `ConcurrentHashMap` is thread-safe, cache operations also modify the eviction policy.

A single lock ensures both data structures remain consistent during updates.

---

## Why an EvictionPolicy Interface?

Separating eviction logic from cache storage makes the implementation open for extension.

New eviction policies can be introduced without modifying `InMemoryCache`.

This follows the Open/Closed Principle.

---

## Why LRU?

Least Recently Used (LRU) is one of the most widely adopted cache eviction algorithms because recently accessed entries are statistically more likely to be accessed again.

---
## Known Limitations

Version 1.0.0 focuses on providing a clean, thread-safe in-memory cache for a single JVM.

Current limitations include:

- Designed for a single JVM; distributed caching is out of scope.
- Does not persist cache contents across application restarts.
- Uses a dedicated cleanup scheduler per cache instance; Version 2 will introduce a shared scheduler.
- The project prioritizes simplicity and extensibility over advanced lock-free concurrency techniques.

# Future Roadmap

Version 2 is planned to include:

- Shared cleanup scheduler for multiple caches
- Additional eviction policies
    - FIFO
    - LFU
    - MRU
- Persistent cache storage
- Builder Pattern for cache configuration
- Serialization support
- Performance benchmarking
- JUnit test suite
- GitHub Actions CI pipeline

---

# Learning Outcomes

This project was built to deepen understanding of:

- Concurrent programming
- Thread safety
- Synchronization
- Locking strategies
- Cache design
- Eviction algorithms
- Java Generics
- Clean API design
- Object-oriented design principles
- Software architecture

---

# License

This project is released under the MIT License.

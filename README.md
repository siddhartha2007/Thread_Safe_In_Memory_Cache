# Thread-Safe In-Memory Cache

A thread-safe, configurable in-memory cache library built in Java.

This project implements an in-process cache designed to provide fast
in-memory data access and reduce repeated database calls. It supports
TTL-based expiration, capacity-based eviction, pluggable eviction
policies, background cleanup, cache metrics, and concurrent cache
operations.

The project focuses on understanding and implementing the concurrency,
data-structure, and design challenges involved in building a reusable
cache library.
---
## Why this project?

Caching is a fundamental technique in backend systems for improving responsiveness by avoiding repeated access to expensive resources such as databases, file systems, or external services.

Instead of relying on an existing caching library, I built this project from scratch to better understand the data structures, concurrency mechanisms, and architectural decisions involved in designing a thread-safe in-memory cache.

This project was built to understand how an in-memory cache can be
designed and implemented from the ground up rather than relying on an
existing caching solution.

The project focuses on practical software engineering concepts including:

- Thread safety and concurrent access
- Java concurrency utilities
- Cache eviction strategies and their underlying data structures
- TTL-based expiration
- Background task scheduling
- Maintaining consistency across multiple internal data structures
- Designing configurable and extensible components
- Writing unit, concurrent, stress, and regression tests
- Identifying and handling race conditions and edge cases

The project has evolved incrementally, with each version introducing
new capabilities while maintaining and testing the behavior of the
existing implementation.

## Features

- Thread-safe cache operations
- TTL-based entry expiration
- Background cleanup of expired entries
- Shared cleanup scheduler across cache instances
- Per-cache cleanup task cancellation
- Capacity-based eviction
- Pluggable eviction policies:
  - LRU — Least Recently Used
  - FIFO — First In, First Out
  - MRU — Most Recently Used
  - LFU — Least Frequently Used
- Builder Pattern for cache configuration
- Cache statistics:
  - Hits
  - Misses
  - Evictions
  - Expired entries
- Concurrent access support
- Unit tests
- Concurrent and stress tests
- Regression tests
- JavaDoc documentation
---

## Architecture

The cache is composed of several independent components, each with a
specific responsibility.

                         +----------------------+
                         |      Client Code     |
                         +----------+-----------+
                                    |
                                    v
                     +-----------------------------+
                     |      InMemoryCache<K,V>     |
                     +-----------------------------+
                     |                             |
                     |  ConcurrentHashMap          |
                     |  CacheMetrics               |
                     |  EvictionPolicy             |
                     |  ReentrantLock              |
                     |  ScheduledFuture            |
                     |                             |
                     +------+-------------+--------+
                            |             |
                            |             |
                            v             v
                 +----------------+   +----------------------+
                 |  CacheEntry    |   |   EvictionPolicy     |
                 +----------------+   +----------------------+
                 | value          |   | LRU                  |
                 | expiry time    |   | FIFO                 |
                 | access count   |   | MRU                  |
                 | last access    |   | LFU                  |
                 +----------------+   +----------------------+

                                    |
                                    v
                         +----------------------+
                         |  CleanUpScheduler    |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | ScheduledExecutor-   |
                         | Service              |
                         +----------------------+
                           /        |        \
                          /         |         \
                         v          v          v
                     Cache A     Cache B     Cache C
                     cleanup     cleanup     cleanup


### Architecture Components

**InMemoryCache**  
The central component responsible for cache operations, TTL handling,
capacity management, eviction, metrics, and coordination with the
cleanup scheduler.

**CacheEntry**  
Represents an individual cached value and stores the information required
for expiration and access tracking.

**EvictionPolicy**  
Defines the eviction strategy used when the cache reaches its configured
capacity. The current implementation supports LRU, FIFO, MRU, and LFU.

**CacheMetrics**  
Maintains cache statistics including hits, misses, evictions, and expired
entries.

**CleanUpScheduler**  
A shared scheduler responsible for periodically executing cleanup tasks
for multiple cache instances. Each cache receives its own
`ScheduledFuture`, allowing its cleanup task to be cancelled independently.

**ScheduledExecutorService**  
Provides the threads used to execute the registered background cleanup
tasks.


## Project Structure

 ```text
src/
├── main/
│   └── java/
│       ├── cache/
│       │   └── Cache.java
│       │
│       ├── implementation/
│       │   └── InMemoryCache.java
│       │
│       ├── model/
│       │   └── CacheEntry.java
│       │
│       ├── eviction/
│       │   ├── EvictionPolicy.java
│       │   ├── LRUEvictionPolicy.java
│       │   ├── FIFOEvictionPolicy.java
│       │   ├── MRUEvictionPolicy.java
│       │   └── LFUEvictionPolicy.java
│       │
│       ├── metrics/
│       │   ├── CacheMetrics.java
│       │   └── CacheStats.java
│       │
│       └── scheduler/
│           └── CleanUpScheduler.java
│
└── test/
    └── java/
        ├── CacheEntryTest/
        ├── CacheMetricsTest/
        └── FIFOEvictionPolicyTest
        ├── InMemoryCacheBuilderTest/
        ├── InMemoryCache2Test/
        └── LFUEvictionPolicyTest/
        ├── LRUEvictionTest/
        └── MRUEvictionPolicyTest/
```

The project follows a modular structure where cache storage, eviction
strategies, metrics, scheduling, and cache entry representation are
separated into independent packages.

This separation allows eviction policies and other components to evolve
without tightly coupling them to the core cache implementation.

## Threading Model

The cache is designed to support concurrent access while maintaining
consistency between its internal data structures.

### Cache Operations

The cache uses a `ConcurrentHashMap` for storing cache entries, allowing
multiple threads to perform concurrent read operations.

Operations that modify both the cache storage and the eviction policy use
a `ReentrantLock` to keep these structures consistent.

For example, during an insertion:

```text
                 put(key, value)
                       |
                       v
                 acquire lock
                       |
             +---------+---------+
             |                   |
             v                   v
        Cache Map          Eviction Policy
             |                   |
             +---------+---------+
                       |
                       v
                  release lock
```

This prevents the cache map and eviction policy from reaching an
inconsistent state when multiple threads modify the cache concurrently.

### Background Cleanup

Expired entries are removed by background cleanup tasks executed by the
shared `CleanUpScheduler`.

```text
                  CleanUpScheduler
                         |
              ScheduledExecutorService
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
       Cache A        Cache B        Cache C
      cleanup task   cleanup task   cleanup task
```

Each cache has its own scheduled cleanup task, represented by a
`ScheduledFuture`.

Cancelling one cache's cleanup task does not affect the cleanup tasks
belonging to other cache instances.

### Eviction Policy Thread Safety

Eviction policies maintain internal data structures such as hash maps
and doubly linked lists.

Their public operations are synchronized to ensure that concurrent
accesses cannot corrupt these structures.

### Metrics

Cache statistics are maintained using thread-safe counters so that
multiple cache operations can update metrics concurrently.

The cache tracks:

- Cache hits
- Cache misses
- Evictions
- Expired entries

---
## Eviction Policies

The cache uses a pluggable `EvictionPolicy` abstraction, allowing the
eviction strategy to be changed without modifying the core cache
implementation.

The following eviction policies are currently supported:

| Policy | Description |
|---|---|
| **LRU** | Evicts the least recently used entry. |
| **FIFO** | Evicts the entry that was inserted first. |
| **MRU** | Evicts the most recently used entry. |
| **LFU** | Evicts the least frequently used entry. |

### LRU — Least Recently Used

LRU tracks the access order of entries.

When an entry is accessed, it becomes the most recently used entry.
When eviction is required, the least recently used entry is selected.

The implementation uses a hash map and a doubly linked list to track
entries efficiently.

### FIFO — First In, First Out

FIFO evicts entries according to their insertion order.

Accessing an entry does not change its position in the eviction order.

The oldest entry is selected when eviction is required.

### MRU — Most Recently Used

MRU is the opposite of LRU.

The most recently accessed entry is selected for eviction.

The implementation maintains access ordering using a doubly linked list.

### LFU — Least Frequently Used

LFU evicts the entry with the lowest access frequency.

The implementation maintains separate frequency buckets. Within each
frequency bucket, entries are ordered by recency.

Therefore, when multiple entries have the same frequency, the least
recently used entry among them is evicted.

```text
Frequency 1:  A → B → C
Frequency 2:  D → E
Frequency 3:  F

                 ↓

          LFU selects A

```
## TTL & Background Cleanup

Each cache entry can have a time-to-live (TTL), after which the entry is
considered expired.

A TTL can be specified when inserting an entry:

```java
cache.put(1, "Siddhartha", 5000);
```

The cache supports two mechanisms for removing expired entries.

### Lazy Expiration

When an expired entry is accessed through operations such as `get()` or
`containsKey()`, the cache detects the expiration and removes the entry.

```text
get(key)
   |
   v
Entry exists?
   |
   +---- No ----> Cache miss
   |
  Yes
   |
   v
Expired?
   |
   +---- Yes ----> Remove entry
   |                 |
   |                 v
   |             Cache miss
   |
  No
   |
   v
Return value
```

### Background Cleanup

Expired entries that are not accessed can remain in the cache until the
background cleanup task runs.

Each `InMemoryCache` registers a periodic cleanup task with the shared
`CleanUpScheduler`.

```text
                 CleanUpScheduler
                        |
                        v
              ScheduledExecutorService
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
       Cache A       Cache B       Cache C
       cleanup       cleanup       cleanup
          |             |             |
          v             v             v
      Remove        Remove        Remove
      expired       expired       expired
      entries       entries       entries
```

The cleanup interval is configurable when creating the cache. If it is not
specified through the Builder Pattern, the cache uses its default cleanup
interval.

### Per-Cache Cleanup Control

Each cache receives a `ScheduledFuture` representing its own cleanup task.

Therefore, cleanup can be stopped for an individual cache:

```java
cache.stopCleanupScheduler();
```

This cancels only that cache's scheduled cleanup task. The shared
scheduler and cleanup tasks belonging to other cache instances continue
running.

## Builder Pattern & Configuration

The cache provides a Builder Pattern for creating and configuring
`InMemoryCache` instances.

The builder provides a readable way to configure the cache while avoiding
increasingly complex constructor overloads.

### Configuration Options

| Configuration | Required | Default |
|---|---|---|
| **Eviction Policy** | Yes | — |
| **Capacity** | No | `100` |
| **Cleanup Interval** | No | `20,000 ms` |

The eviction policy must be explicitly provided. Capacity and cleanup
interval are optional and use their default values when they are not
configured.

### Creating a Builder

A builder can be created using the static `builder()` method:

```java
InMemoryCache.InMemoryCacheBuilder<Integer, String> builder =
        InMemoryCache.InMemoryCacheBuilder.builder();
```

### Configuring the Cache

The builder provides methods for configuring the eviction policy,
capacity, and background cleanup interval:

```java
InMemoryCache<Integer, String> cache =
        InMemoryCache.InMemoryCacheBuilder.<Integer, String>builder()
                .evictionPolicy(new LRUEvictionPolicy<>())
                .capacity(100)
                .cleanUpIntervalMillis(20_000L)
                .build();
```

### Using Default Configuration

Capacity and cleanup interval can be omitted when the default values
are sufficient:

```java
InMemoryCache<Integer, String> cache =
        InMemoryCache.InMemoryCacheBuilder.<Integer, String>builder()
                .evictionPolicy(new LRUEvictionPolicy<>())
                .build();
```

In this case:

- Capacity defaults to `100`.
- Cleanup interval defaults to `20,000 ms`.
- An eviction policy must still be provided.

If no eviction policy is configured, `build()` throws an
`IllegalStateException`.

### Why Builder Pattern?

The Builder Pattern provides a flexible and readable way to configure
the cache as the number of configurable properties grows. It also
allows optional configuration to use sensible defaults while keeping
object construction explicit and easy to understand.

## Usage

### Creating a Cache

A cache can be created using the Builder Pattern:

```java
InMemoryCache<Integer, String> cache =
        InMemoryCache.InMemoryCacheBuilder.<Integer, String>builder()
                .evictionPolicy(new LRUEvictionPolicy<>())
                .capacity(100)
                .cleanUpIntervalMillis(20_000L)
                .build();
```

### Storing Values

```java
cache.put(1, "Siddhartha");
cache.put(2, "Hello");
```

A TTL can be specified for an individual entry:

```java
cache.put(3, "Temporary Value", 5_000L);
```

The entry will be considered expired after the specified TTL.

### Retrieving Values

```java
String value = cache.get(1);
```

If the key does not exist or the entry has expired, `get()` returns
`null`.

### Checking for a Key

```java
boolean exists = cache.containsKey(1);
```

### Removing an Entry

```java
cache.remove(1);
```

### Checking Cache Size

```java
int size = cache.size();
```

### Clearing the Cache

```java
cache.clear();
```

### Retrieving Cache Statistics

```java
CacheStats stats = cache.getStats();

System.out.println("Hits: " + stats.hits());
System.out.println("Misses: " + stats.misses());
System.out.println("Evictions: " + stats.evictions());
System.out.println("Expired Entries: " + stats.expiredentries());
```

### Stopping Background Cleanup

Background cleanup can be stopped for an individual cache instance:

```java
cache.stopCleanupScheduler();
```

This cancels only that cache's scheduled cleanup task. Other cache
instances using the shared cleanup scheduler are unaffected.

### Shutting Down a Cache

When the cache is no longer needed:

```java
cache.shutdown();
```

After shutdown, cache operations are no longer accepted.

## Testing

The project includes a comprehensive test suite covering functional
correctness, concurrency, asynchronous cleanup, eviction policies, and
regression scenarios.

### Unit Tests

Core cache functionality is tested, including:

- `put()`
- `get()`
- `remove()`
- `containsKey()`
- `size()`
- `clear()`
- TTL expiration
- Cache capacity
- Cache statistics
- Builder configuration
- Invalid configuration

### Eviction Policy Tests

Each eviction policy has dedicated tests covering:

- Insertion
- Access ordering
- Eviction behavior
- Removal of entries
- Clearing the policy
- Empty policy behavior
- Duplicate key handling

The following policies are tested:

- LRU
- FIFO
- MRU
- LFU

### Concurrent Tests

Concurrent tests verify behavior when multiple threads operate on the
cache simultaneously.

These tests cover scenarios such as:

- Concurrent `put()` operations
- Concurrent `get()` operations
- Concurrent removal
- Concurrent `clear()` and `get()`
- Concurrent eviction
- Concurrent background cleanup

### Stress Tests

Stress tests execute a large number of concurrent cache operations to
increase the likelihood of exposing race conditions and inconsistent
internal states.

### Regression Tests

Regression tests are added for bugs discovered during development.

For example, a failed `put()` operation must not leave the eviction
policy tracking a key that was never successfully inserted into the
cache.

This ensures that previously fixed bugs are detected if they are
reintroduced during future changes.

### Asynchronous Cleanup Tests

Background cleanup is tested using asynchronous assertions to verify
that expired entries are eventually removed and that cleanup tasks
operate independently for different cache instances.

### Testing Philosophy

The tests are not limited to checking expected return values. They also
verify internal consistency and invariants between the cache storage and
the eviction policy.

The goal is to ensure that concurrent operations and failure scenarios
do not leave the cache in an inconsistent state.

## Known Limitations & Design Trade-offs

### In-Process Cache

The cache is local to a single JVM process.

It is not a distributed cache, so separate application instances maintain
independent cache contents.

```text
Application Instance A          Application Instance B
        |                                |
        v                                v
     Cache A                           Cache B
        |                                |
        +---------- separate ------------+
```

### No Persistence

Cache contents currently exist only in memory.

When the application or JVM terminates, cached entries are lost.

Persistent cache storage is planned for a future version.

### Mutable Cached Values

The cache provides thread safety for its own internal data structures.
It does not automatically make mutable objects stored as values
thread-safe.

For example:

```java
cache.put(1, new ArrayList<>());
```

does not make the `ArrayList` safe for concurrent modification.

Clients are responsible for managing concurrent access to mutable values.

### TOCTOU Window

Some cache operations intentionally avoid acquiring the main
`ReentrantLock` for read-only access.

This improves concurrency but means that a small time-of-check to
time-of-use window can exist between reading an entry and using it.

For example:

```text
Thread A                         Thread B

get(key)
   |
   | entry found
   |
   |                         clear()
   |
   | return entry
   v
```

Therefore, the cache does not attempt to provide strict linearizable
semantics for every operation.

### LFU Minimum-Frequency Maintenance

The LFU eviction policy maintains frequency buckets for cached entries.

When the current minimum-frequency bucket becomes empty after certain
operations, the implementation may scan the remaining frequency buckets
to determine the new minimum frequency.

Therefore, not every LFU operation has strict O(1) complexity.

This is a deliberate trade-off in favor of a simpler and more
maintainable implementation.

### Shared Cleanup Scheduler

All cache instances share the same cleanup scheduler.

This reduces the overhead of maintaining a separate scheduler for every
cache instance, but it also means that the scheduler is a shared
application-level resource.

Individual cache cleanup tasks can be cancelled independently without
shutting down the shared scheduler.

## Roadmap

### Version 2.0.0

- [x] Thread-safe cache operations
- [x] TTL-based expiration
- [x] Background cleanup
- [x] Shared cleanup scheduler
- [x] Builder Pattern
- [x] LRU eviction policy
- [x] FIFO eviction policy
- [x] MRU eviction policy
- [x] LFU eviction policy
- [x] Cache metrics
- [x] Unit tests
- [x] Concurrent tests
- [x] Stress tests
- [x] Regression tests
- [x] JavaDoc documentation

### Future Improvements

- [ ] Pluggable persistence
- [ ] Kryo-based persistence implementation
- [ ] Cache recovery after application restart
- [ ] Additional eviction policies
- [ ] Performance benchmarking
- [ ] Further concurrency and performance optimizations
- [ ] Additional observability features

---

## Version

**Current Version: 2.0.0**

## License

This project is licensed under the MIT License.

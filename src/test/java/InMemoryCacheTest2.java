import eviction.EvictionPolicy;
import eviction.LRUEvictionPolicy;
import exceptions.InvalidTtlException;
import implementation.InMemoryCache;
import metrics.CacheStats;
import model.CacheEntry;
import net.bytebuddy.implementation.bytecode.Throw;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.assertj.core.error.ShouldBeEmptyDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.util.Random;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(MockitoExtension.class)
public class InMemoryCacheTest2 {


    private InMemoryCache<Integer,String> inMemoryCache;


    @Mock
    EvictionPolicy<Integer> evictionPolicy;

    @AfterEach
    void destroy(){
        if(inMemoryCache!=null) {
            inMemoryCache.shutdown();
        }
    }


    @ParameterizedTest
    @ValueSource(ints = {-1,0,-10})
    void shouldThrowIllegalArgumentExceptionWhenNonPositiveCapacityPassed(int capacity){
        assertThrows(IllegalArgumentException.class,() -> new InMemoryCache<>(10_000,capacity,evictionPolicy));

    }


    @ParameterizedTest
    @ValueSource(longs  = {-10000,-5000,0})
    void shouldThrowIllegalArgumentExceptionWhenNonPositiveCleanUpIntervalPassed(long cleanUpIntervalMillis){
        assertThrows(IllegalArgumentException.class,()->new InMemoryCache<>(cleanUpIntervalMillis,2,evictionPolicy));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNullPassedAsEvictionPolicy(){
        assertThrows(IllegalArgumentException.class,()->new InMemoryCache<>(1000,10,null));
    }

    @ParameterizedTest
    @CsvSource({"10000,100",
            "20000,120",
            "15000,130"})
    void shouldConstructCacheUnderValidConfiguration(long cleanUpIntervalMillis,int capacity){
        inMemoryCache=new InMemoryCache<>(cleanUpIntervalMillis,capacity,evictionPolicy);
        assertThat(inMemoryCache).isNotNull();
    }


    @Test
void shouldCallOnInsertWhenNewEntryPassedToCache(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        verify(evictionPolicy).onInsert(eq(1));
        verify(evictionPolicy,never()).onAccess(any());
        verify(evictionPolicy,never()).evict();
        assertThat(inMemoryCache.get(1)).isEqualTo("Siddhartha");
        verify(evictionPolicy,times(1)).onAccess(eq(1));
}


@Test
void shouldIncrementEvictionMetricsWhenEntryEvictedAtCapacity(){
        evictionPolicy=spy(new LRUEvictionPolicy<>());
        inMemoryCache=new InMemoryCache<>(100,1,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        CacheStats stats=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats.expiredentries()).isEqualTo(0),
            ()->assertThat(stats.misses()).isEqualTo(0),
            ()->assertThat(stats.hits()).isEqualTo(0),
            ()->  assertThat(stats.evictions()).isEqualTo(0));
    inMemoryCache.put(2,"Siddhartha");
    verify(evictionPolicy).evict();
    CacheStats stats2=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats2.expiredentries()).isEqualTo(0),
            ()->assertThat(stats2.misses()).isEqualTo(0),
            ()->assertThat(stats2.hits()).isEqualTo(0),
            ()->  assertThat(stats2.evictions()).isEqualTo(1));

}

@Test
void shouldIncrementHitMetricsWhenEntrySuccessullyAccessed(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
    CacheStats stats=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats.expiredentries()).isEqualTo(0),
            ()->assertThat(stats.misses()).isEqualTo(0),
            ()->assertThat(stats.hits()).isEqualTo(0),
            ()->  assertThat(stats.evictions()).isEqualTo(0));

    String s = inMemoryCache.get(1);
    CacheStats stats2=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats2.expiredentries()).isEqualTo(0),
            ()->assertThat(stats2.misses()).isEqualTo(0),
            ()->assertThat(stats2.hits()).isEqualTo(1),
            ()->  assertThat(stats2.evictions()).isEqualTo(0));

}

@Test
void shouldIncrementMissesWhenNon_ExistingEntryAccessed() {
    inMemoryCache = new InMemoryCache<>(1000, 100, evictionPolicy);
    CacheStats stats=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats.expiredentries()).isEqualTo(0),
            ()->assertThat(stats.misses()).isEqualTo(0),
            ()->assertThat(stats.hits()).isEqualTo(0),
            ()->  assertThat(stats.evictions()).isEqualTo(0));
    inMemoryCache.get(1);
    CacheStats stats2=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats2.expiredentries()).isEqualTo(0),
            ()->assertThat(stats2.misses()).isEqualTo(1),
            ()->assertThat(stats2.hits()).isEqualTo(0),
            ()->  assertThat(stats2.evictions()).isEqualTo(0));

}

@Test
void shouldIncrementMissesAndExpiredEntriesWhenExpiredEntryAccessed(){
        inMemoryCache=new InMemoryCache<>(1000000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        clearInvocations(evictionPolicy);
    CacheStats stats=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats.expiredentries()).isEqualTo(0),
            ()->assertThat(stats.misses()).isEqualTo(0),
            ()->assertThat(stats.hits()).isEqualTo(0),
            ()->  assertThat(stats.evictions()).isEqualTo(0));
                await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(100,MILLISECONDS)
                .until(() -> inMemoryCache.get(1)==null);
    CacheStats stats2=inMemoryCache.getStats();
    assertAll( ()-> assertThat(stats2.expiredentries()).isEqualTo(1),
            ()->assertThat(stats2.misses()).isEqualTo(1),
            ()->assertThat(stats2.hits()).isGreaterThan(0),
            ()->  assertThat(stats2.evictions()).isEqualTo(0));
}

@Test
void shouldEvictVictimWhenNewEntryIsInsertedAtCapacity(){
        inMemoryCache=new InMemoryCache<>(10000,1,evictionPolicy);
        when(evictionPolicy.evict()).thenReturn(1);
        inMemoryCache.put(1,"Siddhartha");
        inMemoryCache.put(2,"Santosh");
        CacheStats stats=inMemoryCache.getStats();
        verify(evictionPolicy).evict();
        verify(evictionPolicy).onInsert(1);
        verify(evictionPolicy).onInsert(2);
        verify(evictionPolicy,never()).onAccess(any());
        assertThat(stats.evictions()).isEqualTo(1);
        assertThat(inMemoryCache.containsKey(1)).isFalse();
        assertThat(inMemoryCache.containsKey(2)).isTrue();
}


@Test
    void shouldReplaceValueAndNotifyAccessWhenExistingKeyIsUpdated(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        assertThat(inMemoryCache.get(1)).isEqualTo("Siddhartha");
        inMemoryCache.put(1,"Afsana");
        verify(evictionPolicy,times(1)).onInsert(eq(1));
        verify(evictionPolicy,never()).evict();
        assertThat(inMemoryCache.get(1)).isEqualTo("Afsana");
    verify(evictionPolicy,times(3)).onAccess(eq(1));
}



@Test
    void shouldReturnNullWhenExpiredEntryIsAccessed() {
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",2000);
        await()
                .atMost(Duration.ofSeconds(10))
                        .pollInterval(100,MILLISECONDS)
                                .until(() -> inMemoryCache.containsKey(1)==false);
        assertThat(inMemoryCache.get(1)).isNull();
}

@Test
    void shouldNotifyEvictionPolicyOnceExpiredEntryAccessed(){
        inMemoryCache=new InMemoryCache<>(100_000,20,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",200);
        clearInvocations(evictionPolicy);
        await().atMost(10,SECONDS).pollInterval(50,MILLISECONDS).until(()-> {
           return  inMemoryCache.get(1) == null;
        });
        verify(evictionPolicy).onRemove(1);
}

@Test
    void  shouldReturnNullWhenMissingKeyIsAccessed(){
        inMemoryCache=new InMemoryCache<>(10000,3,evictionPolicy);
        assertThat(inMemoryCache.get(1)).isNull();
}

@Test
    void shouldReturnCorrectEntryValueWhenAccessed(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        assertThat(inMemoryCache.get(1)).isEqualTo("Siddhartha");
}

@Test
    void shouldNotifyEvictionPolicyWhenExistingEntryAccessed(){
        inMemoryCache=new InMemoryCache<>(10000,20,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        clearInvocations(evictionPolicy);
        assertThat(inMemoryCache.get(1)).isEqualTo("Siddhartha");
        verify(evictionPolicy).onAccess(1);
}

@Test
    void shouldIncrementHitCountWhenExistingEntryIsAccessed(){
        inMemoryCache=new InMemoryCache<>(1000000,50,evictionPolicy);
        CacheStats stats=inMemoryCache.getStats();
        assertThat(stats.hits()).isEqualTo(0);
        inMemoryCache.put(1,"Siddhartha");
       inMemoryCache.get(1);
       inMemoryCache.get(1);
       inMemoryCache.get(1);
       final CacheStats stats2=inMemoryCache.getStats();
       assertAll(()->assertThat(stats2.hits()).isEqualTo(3),
               ()->assertThat(stats2.misses()).isEqualTo(0),
               ()->assertThat(stats2.expiredentries()).isEqualTo(0),
               ()->assertThat(stats2.evictions()).isEqualTo(0));
}


@Test
    void shouldUpdateMissesWhenMissingEntryAccessed(){
        inMemoryCache=new InMemoryCache<>(1000,50,evictionPolicy);
        CacheStats stats=inMemoryCache.getStats();
        assertThat(stats.misses()).isEqualTo(0);
        inMemoryCache.get(1);
       final CacheStats stats2=inMemoryCache.getStats();
       assertAll(()->assertThat(stats2.misses()).isEqualTo(1),
               ()->assertThat(stats2.evictions()).isEqualTo(0),
               ()->assertThat(stats2.hits()).isEqualTo(0),
               ()->assertThat(stats2.expiredentries()).isEqualTo(0));

}

@Test
    void shouldUpdateMissesAndExpiriesWhenExpiredEntryAccessed(){
        inMemoryCache=new InMemoryCache<>(90000,500,evictionPolicy);
    CacheStats stats=inMemoryCache.getStats();
    assertThat(stats.misses()).isEqualTo(0);
    assertThat(stats.expiredentries()).isEqualTo(0);
    inMemoryCache.put(1,"Afsana",2000);
    await()
            .pollInterval(100,MILLISECONDS)
                    .atMost(Duration.ofSeconds(10))
                            .until(()->inMemoryCache.get(1)==null);
    final CacheStats stats2=inMemoryCache.getStats();
    assertAll(()->  assertThat(stats2.expiredentries()).isEqualTo(1),
            ()-> assertThat(stats2.misses()).isEqualTo(1),
            ()-> assertThat(stats2.evictions()).isEqualTo(0),
            ()-> assertThat(stats2.hits()).isGreaterThan(0));
}


@Test
    void shoudUpdateEvictionsWhenEntryEvictedAtCapacity(){
        inMemoryCache=new InMemoryCache<>(100,1,evictionPolicy);
        CacheStats stats=inMemoryCache.getStats();
        assertThat(stats.evictions()).isEqualTo(0);
        inMemoryCache.put(1,"Siddhartha");
        when(evictionPolicy.evict()).thenReturn(1);
        inMemoryCache.put(2,"Afsana");
        stats=inMemoryCache.getStats();
        assertThat(stats.evictions()).isEqualTo(1);
}


@Test
    void shouldDeleteEntryFromCacheWhenRemoveCalled(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        inMemoryCache.remove(1);
        assertThat(inMemoryCache.get(1)).isEqualTo(null);
}

@Test
    void shouldNotifyEvictionPolicyWhenEntryRemoved(){
        inMemoryCache=new InMemoryCache<>(1000,5,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        clearInvocations(evictionPolicy);
        inMemoryCache.remove(1);
        verify(evictionPolicy).onRemove(1);
    }


    @Test
    void shouldReturnTrueWhenExistingKeyIsChecked(){
        inMemoryCache=new InMemoryCache<>(1000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha");
        assertThat(inMemoryCache.containsKey(1)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenMissingKeyIsChecked(){
        inMemoryCache=new InMemoryCache<>(1000,3,evictionPolicy);
        assertThat(inMemoryCache.containsKey(1)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenExpiredEntryIsChecked(){
        inMemoryCache=new InMemoryCache<>(1000000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(100,MILLISECONDS)
                .until(()-> !inMemoryCache.containsKey(1));
        assertThat(inMemoryCache.containsKey(1)).isFalse();
    }

    @Test
    void shouldUpdateEvictionPolicyWhenExpiredEntryisChecked(){
        inMemoryCache=new InMemoryCache<>(1000000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        clearInvocations(evictionPolicy);
        await()
                .atMost(Duration.ofSeconds(2))
                        .pollInterval(100,MILLISECONDS)
                                .until(()-> !inMemoryCache.containsKey(1));
        verify(evictionPolicy).onRemove(1);
    }

    @Test
    void shouldUpdateMetricsWhenExpiredEntryIsChecked() {
        inMemoryCache=new InMemoryCache<>(1000000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        await()
                .atMost(Duration.ofSeconds(5))
                        .pollInterval(100,MILLISECONDS)
                                .until(()-> !inMemoryCache.containsKey(1));
       final CacheStats stats2=inMemoryCache.getStats();
        assertAll( ()-> assertThat(stats2.expiredentries()).isEqualTo(1),
                ()->assertThat(stats2.misses()).isEqualTo(0),
                ()->assertThat(stats2.hits()).isEqualTo(0),
                ()->  assertThat(stats2.evictions()).isEqualTo(0));
    }

    @Test
    void shouldReturnExactSizeOfCache(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        for(int i=0;i<5;i++){
            inMemoryCache.put(i,"Siddhartha "+i);
        }
        assertThat(inMemoryCache.size()).isEqualTo(5);
        for(int i=5;i<10;i++){
            inMemoryCache.put(i,"Siddhartha "+i);
        }
        assertThat(inMemoryCache.size()).isEqualTo(10);
    }
    @Test
    void shouldClearContentsOfCacheWhenClearCalled(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        for(int i=0;i<10;i++){
            inMemoryCache.put(i,"Siddhartha "+i);
        }
        assertThat(inMemoryCache.size()).isEqualTo(10);
        inMemoryCache.clear();
        assertThat(inMemoryCache.size()).isEqualTo(0);
    }
    @Test
    void shouldNotifyEvictionPolicyWhenClearCalled(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        for(int i=0;i<10;i++){
            inMemoryCache.put(i,"Siddhartha "+i);
        }
        assertThat(inMemoryCache.size()).isEqualTo(10);
        inMemoryCache.clear();
        verify(evictionPolicy,times(1)).clear();
    }
    @Test
    void shouldReturnFalseWhenShutDownNotDone(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        assertThat(inMemoryCache.isShutDown()).isFalse();
    }
    @Test
    void shouldReturnTrueWhenShutDownDone(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThat(inMemoryCache.isShutDown()).isTrue();
    }

    @Test
    void shouldRejectPutAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThrows(IllegalStateException.class,()-> inMemoryCache.put(1,"Siddhartha"));
        assertThrows(IllegalStateException.class,()->inMemoryCache.put(2,"Siddhartha",2000));
    }
    @Test
    void shouldRejectGetAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThrows(IllegalStateException.class,()-> inMemoryCache.get(1));
    }
    @Test
    void shouldRejectRemoveAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThrows(IllegalStateException.class,()->inMemoryCache.remove(1));
    }
    @Test
    void shouldRejectContainsKeyAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(100000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThrows(IllegalStateException.class,()-> inMemoryCache.containsKey(1));
    }
    @Test
    void shouldRejectSizeAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThrows(IllegalStateException.class,()->inMemoryCache.size());
    }
    @Test
    void shouldRejectClearAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.shutdown();
        assertThrows(IllegalStateException.class,()->inMemoryCache.clear());
    }
    @Test
    void shouldStopBackgroundCleanUpAfterStopCleanUpScheduler() throws InterruptedException {
        inMemoryCache = new InMemoryCache<>(50, 10, evictionPolicy);
        for (int i = 0; i < 10; i++) {
            inMemoryCache.put(i, "Siddhartha - " + i, 30);
        }
        inMemoryCache.stopCleanupScheduler();
        Thread.sleep(300);
        assertThat(inMemoryCache.size()).isEqualTo(10);
    }
    @Test
    void shouldAllowShutdownToBeCalledMultipleTimes() {
    inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
    inMemoryCache.shutdown();
    inMemoryCache.shutdown();
    inMemoryCache.shutdown();
    }
    @Test
    void shouldAllowStoppingCleanupSchedulerMultipleTimes() {
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.stopCleanupScheduler();
        inMemoryCache.stopCleanupScheduler();
        inMemoryCache.stopCleanupScheduler();
    }
    @Test
    void shouldAllowClearingMultipleTimes(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.clear();
        inMemoryCache.clear();
    }
    @Test
    void shouldAllowCallingStopCleanUpSchedulerAfterShutDown(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.shutdown();
        inMemoryCache.stopCleanupScheduler();
    }
    @Test
    void shouldAllowCallingShutDownAfterStoppingBackGroundCleanUp(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.stopCleanupScheduler();
        inMemoryCache.shutdown();
    }
    @Test
    void shouldAllowRemovingNonExistingKeys(){
        inMemoryCache=new InMemoryCache<>(1000,10,evictionPolicy);
        inMemoryCache.remove(1);
        inMemoryCache.remove(2);
    }

    @Test
    void shouldNullPointerExceptionWhenKeyisNullWhenPutting(){
        inMemoryCache=new InMemoryCache<>(100,100,evictionPolicy);
        assertThrows(NullPointerException.class,()->inMemoryCache.put(null,"Siddhartha"));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenValueIsNullWhilePutting(){
        inMemoryCache=new InMemoryCache<>(100,1000,evictionPolicy);
        assertThrows(IllegalArgumentException.class,()-> inMemoryCache.put(1,null));
    }

    @Test
    void shouldThrowInvalidTtlExceptionWhenNegativeTtlPassed(){
        inMemoryCache=new InMemoryCache<>(100,1000,evictionPolicy);
         InvalidTtlException invalidTtlException=  assertThrows(InvalidTtlException.class,()->inMemoryCache.put(1,"Siddhartha",0));
         assertThat(invalidTtlException.getMessage()).isEqualTo("TtlMillis cannot be Negative or Zero");
    }


    @Test
    void shouldCleanTheCacheViaBackGroundCleanups(){
        inMemoryCache=new InMemoryCache<>(1000,100,evictionPolicy);
        for(int i=0;i<100;i++){
            inMemoryCache.put(i,"Sidd "+i,50);
        }

        assertThat(inMemoryCache.size()).isEqualTo(100);
        await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(100,MILLISECONDS)
                .until(()->inMemoryCache.size()==0);
        assertThat(inMemoryCache.size()).isZero();
        assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(100);
    }

    // Concurreny Testing
    @Test
    void shouldSuccessfullyPerformConcurrentPuts() throws InterruptedException {
        evictionPolicy=new LRUEvictionPolicy<>();
        inMemoryCache=new InMemoryCache<>(100000,100,evictionPolicy);
        CountDownLatch startLatch=new CountDownLatch(1);
        CountDownLatch endLatch=new CountDownLatch(100);
        ExecutorService executorService= Executors.newFixedThreadPool(20);

        for(int i=0;i<100;i++){
            int key=i;
            executorService.submit(()-> {
                try {
                    startLatch.await();
                    inMemoryCache.put(key,"Siddhartha -"+key);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
                finally {
                    endLatch.countDown();
                }
            });
        }
        executorService.shutdown();
        startLatch.countDown();
        assertThat(endLatch.await(10,SECONDS)).isTrue();
        assertThat(inMemoryCache.size()).isEqualTo(100);

        for(int i=0;i<100;i++){
            assertThat(inMemoryCache.containsKey(i)).isTrue();
        }
    }

    @Test
    void shouldRetainSingleEntryAfterConcurrentUpdatesToSameKey() throws InterruptedException{
        evictionPolicy=new LRUEvictionPolicy<>();
        inMemoryCache=new InMemoryCache<>(100000,100,evictionPolicy);
        CountDownLatch startLatch=new CountDownLatch(1);
        CountDownLatch endLatch=new CountDownLatch(100);
        ExecutorService executorService=Executors.newFixedThreadPool(20);
        int key=1;
        HashSet<String> set=new HashSet<>();
        AtomicReference<Throwable> failure=new AtomicReference<>();
        for(int i=0;i<100;i++){
            int v=i;
            executorService.submit(()->{
                try{
                    startLatch.await();
                    inMemoryCache.put(1,"siddhartha "+v);
                } catch (Throwable t) {
                    failure.compareAndSet(null,t);
                }finally {
                    endLatch.countDown();
                }
            });
            set.add("siddhartha "+v);
        }
        executorService.shutdown();
        startLatch.countDown();
        assertThat(endLatch.await(10,SECONDS)).isTrue();
        assertThat(failure.get()).isNull();
        assertThat(inMemoryCache.size()).isEqualTo(1);
        assertThat(inMemoryCache.containsKey(1)).isTrue();
        assertThat(SetCheck(set, inMemoryCache.get(1))).isTrue();
        assertThat(inMemoryCache.getStats().evictions()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().misses()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().hits()).isEqualTo(1);
        assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(0);
    }
    boolean SetCheck(HashSet<String> set,String value){
        return set.contains(value);
    }

    @Test
    void shouldRetainValueOrRemoveEntryAfterConcurrentPutAndRemove() throws InterruptedException{
        evictionPolicy=new LRUEvictionPolicy<>();
        inMemoryCache=new InMemoryCache<>(100000,100,evictionPolicy);
        CountDownLatch startLatch=new CountDownLatch(1);
        CountDownLatch endLatch=new CountDownLatch(100);
        ExecutorService executorService=Executors.newFixedThreadPool(20);
        int key=1;
        AtomicReference<Throwable> atomicReference =new AtomicReference<>();
        HashSet<String> set=new HashSet<>();
        for(int i=0;i<50;i++){
            int v=i;
            executorService.submit(()->{
                try {
                    startLatch.await();
                    inMemoryCache.put(1,"Siddhartha"+v);
                }catch(Throwable t){
                    atomicReference.compareAndSet(null,t);
                }finally {
                    endLatch.countDown();
                }
            });
            set.add("Siddhartha"+v);
        }

        for(int i=0;i<50;i++){
            executorService.submit(()->{
                try {
                    startLatch.await();
                    inMemoryCache.remove(1);
                }catch(Throwable t){
                    atomicReference.compareAndSet(null,t);
                }finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertThat(endLatch.await(10,SECONDS)).isTrue();
        executorService.shutdown();
        assertThat(atomicReference.get()).isNull();
        assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().hits()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().misses()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().evictions()).isEqualTo(0);
        assertThat(inMemoryCache.size()).isIn(0,1);
        if(inMemoryCache.size()==1) {
            assertThat(inMemoryCache.containsKey(1)).isTrue();
            assertThat(inMemoryCache.get(1)).isNotNull();
            assertThat(set).contains(inMemoryCache.get(1));
        }else{
            assertThat(inMemoryCache.get(1)).isNull();
            assertThat(inMemoryCache.containsKey(1)).isFalse();
        }
    }

    @Test
    void shouldHandleConcurrentGetAndRemoveWithoutExceptions() throws InterruptedException{
        evictionPolicy=new LRUEvictionPolicy<>();
        inMemoryCache=new InMemoryCache<>(10000,100,evictionPolicy);
        CountDownLatch startLatch=new CountDownLatch(1);
        CountDownLatch endLatch=new CountDownLatch(100);
        ExecutorService executorService=Executors.newFixedThreadPool(20);
        AtomicReference<Throwable> atomicReference=new AtomicReference<>();
        inMemoryCache.put(1,"Siddhartha");
        for(int i=0;i<50;i++){
            executorService.submit(() -> {
               try {
                   startLatch.await();
                   inMemoryCache.get(1);
               }catch (Throwable t){
                   atomicReference.compareAndSet(null,t);
               }finally {
                   endLatch.countDown();
               }
            });
        }
        for(int i=0;i<50;i++){
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    inMemoryCache.remove(1);
                }catch (Throwable t){
                    atomicReference.compareAndSet(null,t);
                }finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        executorService.shutdown();
        assertThat(endLatch.await(10,SECONDS)).isTrue();
        assertThat(atomicReference.get()).isNull();
        assertThat(inMemoryCache.size()).isEqualTo(0);
        assertThat(inMemoryCache.containsKey(1)).isFalse();
        assertThat(inMemoryCache.getStats().evictions() + inMemoryCache.getStats().expiredentries()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().hits()+ inMemoryCache.getStats().misses()).isEqualTo(50);
        assertThat(inMemoryCache.get(1)).isNull();
    }

    // no exceptions
    // metrics set to zero only
    // size is zero
    // get() returns null
    // containskey at last is false.

    @Test
    void shouldHandleConcurrentRemoveAndContainsKeyWithoutExceptionsAndInconsistencies() throws  InterruptedException{
        evictionPolicy=new LRUEvictionPolicy<>();
        inMemoryCache=new InMemoryCache<>(100000,100,evictionPolicy);
        CountDownLatch startLatch=new CountDownLatch(1);
        CountDownLatch endLatch=new CountDownLatch(100);
        AtomicReference<Throwable> atomicReference=new AtomicReference<>();
        ExecutorService executorService=Executors.newFixedThreadPool(20);
        inMemoryCache.put(1,"Siddhartha");
        for(int i=0;i<50;i++){
            executorService.submit(() ->{
                try {
                    startLatch.await();
                    inMemoryCache.containsKey(1);
                }catch (Throwable t){
                    atomicReference.compareAndSet(null,t);
                }finally {
                    endLatch.countDown();
                }
            });

            executorService.submit(() ->{
                try {
                    startLatch.await();
                    inMemoryCache.remove(1);
                }catch (Throwable t){
                    atomicReference.compareAndSet(null,t);
                }finally {
                    endLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertThat(endLatch.await(10,SECONDS)).isTrue();
        executorService.shutdown();
        assertThat(atomicReference.get()).isNull();
        assertThat(inMemoryCache.size()).isEqualTo(0);
        assertThat(inMemoryCache.containsKey(1)).isFalse();
        assertThat(inMemoryCache.get(1)).isNull();
        CacheStats stats = inMemoryCache.getStats();

        assertAll(
                () -> assertThat(stats.hits()).isZero(),
                () -> assertThat(stats.misses()).isEqualTo(1),
                () -> assertThat(stats.evictions()).isZero(),
                () -> assertThat(stats.expiredentries()).isZero()
        );
    }

    //the invariants:
    // misses+hits should be 50
    // cache size is 0
    // no Exceptions

    @Test
    void shouldHandleConcurrentGetAndClear() throws InterruptedException {
        evictionPolicy = new LRUEvictionPolicy<>();
        inMemoryCache = new InMemoryCache<>(1000000, 100, evictionPolicy);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(51);
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 100; i++) {
            inMemoryCache.put(i, "Siddhartha" + i);
        }
        AtomicReference<Throwable> atomicReference = new AtomicReference<>();
        for (int i = 0; i < 50; i++) {
            int k = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    inMemoryCache.get(k);
                } catch (Throwable t) {
                    atomicReference.compareAndSet(null, t);
                } finally {
                    endLatch.countDown();
                }
            });
            if (i == 49) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        inMemoryCache.clear();
                    } catch (Throwable t) {
                        atomicReference.compareAndSet(null, t);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
        }
        startLatch.countDown();
        assertThat(endLatch.await(10,SECONDS)).isTrue();
        executorService.shutdown();
        assertThat(atomicReference.get()).isNull();
        assertThat(inMemoryCache.size()).isZero();
        CacheStats stats = inMemoryCache.getStats();

        assertAll(
                () -> assertThat(stats.hits() + stats.misses()).isEqualTo(50),
                () -> assertThat(stats.evictions()).isZero(),
                () -> assertThat(stats.expiredentries()).isZero()
        );
        assertThat(inMemoryCache.containsKey(10)).isFalse();
        assertThat(inMemoryCache.get(10)).isNull();
    }

    @Test
    void shouldRemainConsistentUnderRandomConcurrentStress() throws InterruptedException {

        evictionPolicy = new LRUEvictionPolicy<>();
        inMemoryCache = new InMemoryCache<>(1000000, 100, evictionPolicy);

        int threads = 100;
        int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(20);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {

            executor.submit(() -> {

                Random random = new Random();

                try {

                    startLatch.await();

                    for (int j = 0; j < operationsPerThread; j++) {

                        int key = random.nextInt(150);

                        switch (random.nextInt(4)) {

                            case 0:
                                inMemoryCache.put(key, "Value-" + key);
                                break;

                            case 1:
                                inMemoryCache.get(key);
                                break;

                            case 2:
                                inMemoryCache.remove(key);
                                break;

                            case 3:
                                inMemoryCache.containsKey(key);
                                break;
                        }
                    }

                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                } finally {
                    endLatch.countDown();
                }

            });
        }

        startLatch.countDown();

        assertThat(endLatch.await(30, TimeUnit.SECONDS)).isTrue();

        executor.shutdown();

        assertThat(failure.get()).isNull();

        CacheStats stats = inMemoryCache.getStats();

        assertAll(

                () -> assertThat(inMemoryCache.size()).isLessThanOrEqualTo(100),

                () -> assertThat(stats.hits()).isGreaterThanOrEqualTo(0),

                () -> assertThat(stats.misses()).isGreaterThanOrEqualTo(0),

                () -> assertThat(stats.evictions()).isGreaterThanOrEqualTo(0),

                () -> assertThat(stats.expiredentries()).isGreaterThanOrEqualTo(0)

        );

        // Cache should still be usable after stress.

        inMemoryCache.put(999, "Siddhartha");

        assertThat(inMemoryCache.get(999)).isEqualTo("Siddhartha");

    }

    @Test
    void shouldScheduleABackGroundCleanupWhenCacheInstanceCreated(){
        inMemoryCache=new InMemoryCache<>(100,100,evictionPolicy);
        for(int i=0;i<100;i++){
            inMemoryCache.put(i,"Siddhartha"+i,50);
        }
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(100,MILLISECONDS)
                .until(()->inMemoryCache.size()==0);

        assertThat(inMemoryCache.size()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(100);
        assertThat(inMemoryCache.get(1)).isNull();
        assertThat(inMemoryCache.get(99)).isNull();
        assertThat(inMemoryCache.containsKey(1)).isFalse();
    }

    @Test
    void shouldOnlyStopBackgroundCleanUpForThatParticularCache() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(100,1000,evictionPolicy);
        InMemoryCache<Integer,String> inMemoryCache1=new InMemoryCache<>(100,100,evictionPolicy);
        for(int i=0;i<100;i++){
            inMemoryCache1.put(i,"Siddhartha"+i,100);
            inMemoryCache.put(i,"Siddhartha"+i,50);
        }
        inMemoryCache1.stopCleanupScheduler();
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(100,MILLISECONDS)
                .until(()->inMemoryCache.size()==0);
        assertThat(inMemoryCache.size()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(100);
        assertThat(inMemoryCache.get(1)).isNull();
        assertThat(inMemoryCache.get(99)).isNull();
        assertThat(inMemoryCache.containsKey(1)).isFalse();
        int prevsize=inMemoryCache1.size();
        inMemoryCache1.put(101,"Siddhartha",100);
        inMemoryCache1.put(102,"Afsana",100);
        Thread.sleep(250);
        assertThat(inMemoryCache1.size()).isGreaterThan(0);
        assertThat(inMemoryCache1.size()).isGreaterThan(prevsize);
    }

    @Test
    void shouldAllowMultipleCachesToShareScheduler(){
        inMemoryCache=new InMemoryCache<>(100,100,evictionPolicy);
        InMemoryCache<Integer,String> inMemoryCache1=new InMemoryCache<>(100,100,evictionPolicy);
        InMemoryCache<Integer,String> inMemoryCache2=new InMemoryCache<>(100,100,evictionPolicy);
        for(int i=0;i<100;i++){
            inMemoryCache.put(i,"Siddhartha"+i,50);
            inMemoryCache1.put(i,"Siddhartha"+i,50);
            inMemoryCache2.put(i,"Siddhartha"+i,50);
        }
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(100,MILLISECONDS)
                .until(()-> inMemoryCache1.size()==0 && inMemoryCache2.size()==0 && inMemoryCache.size()==0);
        assertThat(inMemoryCache.size()).isEqualTo(0);
        assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(100);
        assertThat(inMemoryCache.get(1)).isNull();
        assertThat(inMemoryCache.get(99)).isNull();
        assertThat(inMemoryCache.containsKey(1)).isFalse();

        assertThat(inMemoryCache1.size()).isEqualTo(0);
        assertThat(inMemoryCache1.getStats().expiredentries()).isEqualTo(100);
        assertThat(inMemoryCache1.get(1)).isNull();
        assertThat(inMemoryCache1.get(99)).isNull();
        assertThat(inMemoryCache1.containsKey(1)).isFalse();

        assertThat(inMemoryCache2.size()).isEqualTo(0);
        assertThat(inMemoryCache2.getStats().expiredentries()).isEqualTo(100);
        assertThat(inMemoryCache2.get(1)).isNull();
        assertThat(inMemoryCache2.get(99)).isNull();
        assertThat(inMemoryCache2.containsKey(1)).isFalse();

    }
}


import eviction.LRUEvictionPolicy;
import implementation.InMemoryCache;
import metrics.CacheStats;
import model.CacheEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InMemoryCacheTest2 {


    private InMemoryCache<Integer,String> inMemoryCache;


    @Mock
    LRUEvictionPolicy<Integer> evictionPolicy;

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
        assertThat(inMemoryCache).isNull();
    }

    @ParameterizedTest
    @CsvSource({"10000,100",
            "20000,120",
            "15000,130"})
    void shouldConstructCacheUnderValidConfiguration(long cleanUpIntervalMillis,int capacity){
        inMemoryCache=new InMemoryCache<>(cleanUpIntervalMillis,capacity,evictionPolicy);
        assertThat(inMemoryCache).isNotNull();
    }


//    Testing put(Key,Value) method
// three branches - input  existing key, input new key and size()<capacity, input new key and size()>=capacity

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
void shouldEvictVictimWhenNewEntryIsInsertedAtCapacity(){
        inMemoryCache=new InMemoryCache<>(10000,1,evictionPolicy);
        when(evictionPolicy.evict()).thenReturn(1);
        inMemoryCache.put(1,"Siddhartha");
        inMemoryCache.put(2,"Santosh");
        verify(evictionPolicy).evict();
        verify(evictionPolicy).onInsert(1);
        verify(evictionPolicy).onInsert(2);
        verify(evictionPolicy,never()).onAccess(any());
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
    void shouldReturnNullWhenExpiredEntryIsAccessed() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(10000,10,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",2000);
        Thread.sleep(2000);
        assertThat(inMemoryCache.get(1)).isNull();
}

@Test
    void shouldNotifyEvictionPolicyOnceExpiredEntryAccessed() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(500,20,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",2000);
        Thread.sleep(2000);
        inMemoryCache.get(1);
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
    void shouldUpdateHitsWhenExisitingEntryAccessed(){
        inMemoryCache=new InMemoryCache<>(1000,50,evictionPolicy);
        CacheStats stats=inMemoryCache.getStats();
        assertThat(stats.hits()).isEqualTo(0);
        inMemoryCache.put(1,"Siddhartha");
        inMemoryCache.get(1);
       final CacheStats stats2=inMemoryCache.getStats();
       assertAll(()->assertThat(stats2.hits()).isEqualTo(1),
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
    void shouldUpdateMissesAndExpiriesWhenExpiredEntryAccessed() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(90000,500,evictionPolicy);
    CacheStats stats=inMemoryCache.getStats();
    assertThat(stats.misses()).isEqualTo(0);
    assertThat(stats.expiredentries()).isEqualTo(0);
    inMemoryCache.put(1,"Afsana",2000);
    Thread.sleep(2000);
    inMemoryCache.get(1);
    final CacheStats stats2=inMemoryCache.getStats();
    assertAll(()->  assertThat(stats2.expiredentries()).isEqualTo(1),
            ()-> assertThat(stats2.misses()).isEqualTo(1),
            ()-> assertThat(stats2.evictions()).isEqualTo(0),
            ()-> assertThat(stats2.hits()).isEqualTo(0));
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
    void shouldReturnFalseWhenExpiredEntryIsChecked() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(1000000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        Thread.sleep(1000);
//        CacheStats stats=inMemoryCache.getStats();
//        assertThat(stats.expiredentries()).isEqualTo(1);
        assertThat(inMemoryCache.containsKey(1)).isFalse();
    }


    @Test
    void shouldUpdateEvictionPolicyWhenExpiredEntryisChecked() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(1000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        clearInvocations(evictionPolicy);
        Thread.sleep(1000);
        assertThat(inMemoryCache.containsKey(1)).isFalse();
        verify(evictionPolicy).onRemove(1);
    }

    @Test
    void shouldUpdateMetricsWhenExpiredEntryIsChecked() throws InterruptedException{
        inMemoryCache=new InMemoryCache<>(1000,3,evictionPolicy);
        inMemoryCache.put(1,"Siddhartha",1000);
        Thread.sleep(1000);
        CacheStats stats=inMemoryCache.getStats();
        assertThat(stats.expiredentries()).isEqualTo(0);
        inMemoryCache.containsKey(1);
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



}

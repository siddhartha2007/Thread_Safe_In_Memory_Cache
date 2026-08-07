import eviction.LRUEvictionPolicy;
import implementation.InMemoryCache;
import implementation.InMemoryCache.InMemoryCacheBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static implementation.InMemoryCache.InMemoryCacheBuilder.builder;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InMemoryCacheBuilderTest {

    InMemoryCache<Integer,String> inMemoryCache;

    @Test
    void shouldCreateCacheWithDefaultConfiguration() {

        inMemoryCache = InMemoryCacheBuilder.<Integer,String>builder()
                        .evictionPolicy(new LRUEvictionPolicy<>())
                        .build();

        assertThat(inMemoryCache).isNotNull();
    }

    @Test
    void shouldCreateCacheWithCustomConfiguration() {

        inMemoryCache =
                InMemoryCacheBuilder.<Integer, String>builder()
                        .capacity(500)
                        .cleanUpIntervalMillis(500L)
                        .evictionPolicy(new LRUEvictionPolicy<>())
                        .build();

        assertThat(inMemoryCache).isNotNull();
    }

    @Test
    void shouldUseDefaultCapacityWhenCapacityNotSpecified() {

        inMemoryCache =
                InMemoryCacheBuilder.<Integer,String>builder()
                        .evictionPolicy(new LRUEvictionPolicy<>())
                        .build();

        for(int i=0;i<100;i++){
            inMemoryCache.put(i,"Value");
        }

        assertThat(inMemoryCache.size()).isEqualTo(100);

        inMemoryCache.put(101,"A");

        assertThat(inMemoryCache.size()).isEqualTo(100);
    }

    @Test
    @Tag("slow")
    void shouldUseDefaultCleanUpIntervalWhenNotSpecified(){
        inMemoryCache= InMemoryCacheBuilder.<Integer,String>builder()
                .capacity(100)
                .evictionPolicy(new LRUEvictionPolicy<>())
                .build();
        for(int i=0;i<100;i++){
            inMemoryCache.put(i,"Siddhartha"+i,100);
        }

        await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(()->inMemoryCache.size()==0);

        assertAll(
                ()-> assertThat(inMemoryCache.size()).isEqualTo(0),
                ()->assertThat(inMemoryCache.getStats().expiredentries()).isEqualTo(100),
                ()->assertThat(inMemoryCache.getStats().evictions()).isEqualTo(0),
                ()->assertThat(inMemoryCache.getStats().hits()).isEqualTo(0),
                ()->assertThat(inMemoryCache.getStats().misses()).isEqualTo(0),
                ()->assertThat(inMemoryCache.get(1)).isNull(),
                ()->assertThat(inMemoryCache.containsKey(1)).isFalse()
        );

    }

    @Test
    void shouldThrowExceptionWhenEvictionPolicyNotProvided() {

        IllegalStateException illegalStateException=assertThrows(
                IllegalStateException.class,
                () -> InMemoryCacheBuilder.<Integer,String>builder().build()
        );

        assertThat(illegalStateException.getMessage()).isEqualTo("Eviction Policy must be specified.");
    }

    @Test
    void shouldSupportMethodChaining() {

        assertThatCode(() ->
                InMemoryCacheBuilder.<Integer,String>builder()
                        .capacity(100)
                        .cleanUpIntervalMillis(1000L)
                        .evictionPolicy(new LRUEvictionPolicy<>())
                        .build()
        ).doesNotThrowAnyException();
    }




}

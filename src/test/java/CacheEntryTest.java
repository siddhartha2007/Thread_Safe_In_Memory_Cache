import model.CacheEntry;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import metrics.CacheMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eviction.EvictionPolicy;
import eviction.LRUEvictionPolicy;
import exceptions.InvalidTtlException;
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

import java.time.Duration;
import java.time.OffsetTime;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
public class CacheEntryTest {

    CacheEntry<String> entry;

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNullValuePassed(){
        IllegalArgumentException illegalArgumentException=assertThrows(IllegalArgumentException.class,()-> new CacheEntry<>(null));
        IllegalArgumentException illegalArgumentException1=assertThrows(IllegalArgumentException.class,()->new CacheEntry<>(null,10000));
        assertThat(illegalArgumentException.getMessage()).isEqualTo("Value cannot be null");
        assertThat(illegalArgumentException1.getMessage()).isEqualTo("Value cannot be null");
    }

    @Test
    void shouldThrowInvalidTtlExceptionWhenZeroTtlPassed(){
        InvalidTtlException invalidTtlException=assertThrows(InvalidTtlException.class,()-> new CacheEntry<>("Siddhartha",0));
        assertThat(invalidTtlException.getMessage()).isEqualTo("TtlMillis cannot be Negative or Zero");
    }

    @Test
    void shouldThrowInvalidTtlExceptionWhenNonPositiveTtlPassed(){
        InvalidTtlException invalidTtlException=assertThrows(InvalidTtlException.class,()->new CacheEntry<>("Siddhartha",-1));
        assertThat(invalidTtlException.getMessage()).isEqualTo("TtlMillis cannot be Negative or Zero");
    }

    @Test
    void shouldReturnActualValueWhenAccessed(){
        entry=new CacheEntry<>("Siddhartha");
        assertThat(entry.getValue()).isEqualTo("Siddhartha");
        entry=new CacheEntry<>("Santosh");
        assertThat(entry.getValue()).isEqualTo("Santosh");
    }

    @Test
    void shouldIncrmentAccessCountWhenIncrementAccessCountCalled(){
        entry=new CacheEntry<>("Siddhartha");
        for(int i=0;i<5;i++){
            entry.incrementAccessCount();
        }
        assertThat(entry.getAccessCount()).isEqualTo(5);
    }

    @Test
    void shouldUpdateLastAccessTimeWhenCalled(){
        entry=new CacheEntry<>("Siddhartha");
        entry.updateLastAccessTime();
        assertThat(entry.getLastAccessTime()).isCloseTo(System.currentTimeMillis(), within(100L));
    }
    @Test
    void shouldNotExpiryWhenTtlTimeNotSet(){
        entry=new CacheEntry<>("Siddhartha");

        assertThat(entry.isExpired()).isFalse();
    }

    @Test
    void shouldExpireWhenTtlExceeded(){
        entry=new CacheEntry<>("Siddhartha",2000);
        await()
                .atMost(Duration.ofSeconds(3))
                        .pollInterval(50,MILLISECONDS)
                                .until(()-> entry.isExpired());
        assertThat(entry.isExpired()).isTrue();
    }

    @Test
    void shouldNotExpireWhenTtlNotExceeded(){
        entry=new CacheEntry<>("Siddharthe",1000);
        assertThat(entry.isExpired()).isFalse();
    }



}

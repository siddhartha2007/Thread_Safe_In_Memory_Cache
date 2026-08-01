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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
public class CacheMetricsTest {

    private CacheMetrics metrics;

    @BeforeEach
    void setup(){
        metrics=new CacheMetrics();
    }

    @Test
    void shouldIncrementHitsWhenHitsIncremented(){
        for(int i=0;i<5;i++){
            metrics.incrementHits();
        }
        assertThat(metrics.getCacheHits()).isEqualTo(5);
    }

    @Test
    void shouldIncrementMissesWhenIncremented(){
        for(int i=0;i<5;i++){
            metrics.incrementMisses();
        }
        assertThat(metrics.getCacheMisses()).isEqualTo(5);
    }

    @Test
    void shoudIncrementExpiredEntriesWhenIncremented(){
        for(int i=0;i<5;i++){
            metrics.incrementExpiredEntries();
        }
        assertThat(metrics.getExpiredEntries()).isEqualTo(5);
    }

    @Test
    void shouldIncrementEvictsWhenIncremented(){
        for(int i=0;i<5;i++){
            metrics.incrementEvictions();
        }
        assertThat(metrics.getCacheEvictions()).isEqualTo(5);
    }
}

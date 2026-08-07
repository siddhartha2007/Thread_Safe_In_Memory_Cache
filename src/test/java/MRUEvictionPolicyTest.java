import eviction.EvictionPolicy;
import eviction.MRUEvictionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MRUEvictionPolicyTest {
    EvictionPolicy<Integer> evictionPolicy;


    @BeforeEach
    void setup(){
        evictionPolicy=new MRUEvictionPolicy<>();
    }

    @Test
    void shouldInsertKeyWhenOnInsertCalled(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        assertThat(evictionPolicy.evict()).isEqualTo(2);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenDuplicateKeysInserted(){
        evictionPolicy.onInsert(1);
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onInsert(1));
        assertThat(illegalStateException.getMessage()).isEqualTo("Duplicate key inserted into eviction policy.");
    }


    @Test
    void shouldMakeMostRecentlyAccessedGetEvictedWhenMultipleNodesPresent(){
        evictionPolicy.onInsert(1); // head.
        evictionPolicy.onInsert(2); // middle node
        evictionPolicy.onInsert(3); // tail node
        // accessing elements . this modifies the linked list.
        evictionPolicy.onAccess(1);// tail
        evictionPolicy.onAccess(2);// 2-tail,1-middle node

        assertThat(evictionPolicy.evict()).isEqualTo(2);
    }

    @Test
    void shouldEvictOnlyElementPresent(){
        evictionPolicy.onInsert(1);
        for(int i=0;i<5;i++){
            evictionPolicy.onAccess(1);
        }
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }

    void shouldRemoveNodeFromEvictionPolicyWhenCalledOnRemoveOnIt(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onRemove(1);
        assertThat(evictionPolicy.evict()).isNull();
    }

    @Test
    void shouldRemoveTheHeadOnRemove(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onRemove(1);
        assertThat(evictionPolicy.evict()).isEqualTo(3);
        assertThat(evictionPolicy.evict()).isEqualTo(2);
    }

    @Test
    void shouldEvictOnlyTheMostRecentlyUsed(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onAccess(3);
        assertThat(evictionPolicy.evict()).isEqualTo(3);
    }

    @Test
    void shouldRemoveTheMiddleNodeOnRemove(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onRemove(2);

        assertThat(evictionPolicy.evict()).isEqualTo(3);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }

    @Test
    void shouldRemoveTheTailOnRemove(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onRemove(3);

        assertThat(evictionPolicy.evict()).isEqualTo(2);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }

    @Test
    void shouldReturnNullWhenEvictCalledOnEmptyEvictionPolicyState(){
        assertThat(evictionPolicy.evict()).isEqualTo(null);
    }

    @Test
    void shouldReturnTheWhenTailWhenCalledEvict(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        assertThat(evictionPolicy.evict()).isEqualTo(3);
        assertThat(evictionPolicy.evict()).isEqualTo(2);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }

    @Test
    void shouldCleanEvictionPolicyWhenCalledClear() {
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.clear();
        assertThat(evictionPolicy.evict()).isEqualTo(null);
    }

    @Test
    void shouldIgnoreRemovalOfMissingKey() {

        evictionPolicy.onRemove(100);

        assertThat(evictionPolicy.evict()).isNull();
    }



}

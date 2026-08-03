import eviction.EvictionPolicy;
import eviction.LRUEvictionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
public class LruEvictionTest {

    LRUEvictionPolicy<Integer> evictionPolicy;
    @BeforeEach
    public void setup(){
        evictionPolicy=new LRUEvictionPolicy<>();
    }
    @Test
    void shouldInsertKeyWhenOnInsertCalled(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
        assertThat(evictionPolicy.evict()).isEqualTo(2);
    }
    @Test
    void shouldThrowIllegalStateExceptionWhenDuplicateKeysInserted(){
        evictionPolicy.onInsert(1);
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onInsert(1));
        assertThat(illegalStateException.getMessage()).isEqualTo("Duplicate key inserted into eviction policy.");
    }
    // test behaviour when multiple keys inserted.
    @Test
    void shouldThrowIllegalStateExceptionWhenMissingKeyIsAccessed(){
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onAccess(1));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");
    }
    @Test
    void shouldMakeLeastAccessedGetEvictedWhenMultipleNodesPresent(){
        evictionPolicy.onInsert(1); // head.
         evictionPolicy.onInsert(2); // middle node
        evictionPolicy.onInsert(3); // tail node
        // accessing elements . this modifies the linked list.
        evictionPolicy.onAccess(1);// tail
        evictionPolicy.onAccess(2);// 2-tail,1-middle node

        assertThat(evictionPolicy.evict()).isEqualTo(3);
    }
    @Test
    void shouldEvictOnlyElementPresent(){
        evictionPolicy.onInsert(1);
        for(int i=0;i<5;i++){
            evictionPolicy.onAccess(1);
        }
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }

    @Test
    void shouldRemoveNodeFromEvictionPolicyWhenCalledOnRemoveOnIt(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onRemove(1);
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onAccess(1));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");
    }

    @Test
    void shouldRemoveTheHeadOnRemove(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onRemove(1);
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onAccess(1));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");

        assertThat(evictionPolicy.evict()).isEqualTo(2);
        assertThat(evictionPolicy.evict()).isEqualTo(3);
    }

    @Test
    void shouldRemoveOnlyTheLeastRecentlyUsed(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onAccess(3);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
    }


    @Test
    void shouldRemoveTheMiddleNodeOnRemove(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onRemove(2);
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onAccess(2));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");

        assertThat(evictionPolicy.evict()).isEqualTo(1);
        assertThat(evictionPolicy.evict()).isEqualTo(3);
    }

    @Test
    void shouldRemoveTheTailOnRemove(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        evictionPolicy.onRemove(3);
        IllegalStateException illegalStateException=assertThrows(IllegalStateException.class,()-> evictionPolicy.onAccess(3));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");
        assertThat(evictionPolicy.evict()).isEqualTo(1);
        assertThat(evictionPolicy.evict()).isEqualTo(2);
    }

    @Test
    void shouldReturnNullWhenEvictCalledOnEmptyEvictionPolicyState(){
        assertThat(evictionPolicy.evict()).isEqualTo(null);
    }

    @Test
    void shouldReturnTheHeadWhenCalledEvict(){
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.onInsert(3);
        assertThat(evictionPolicy.evict()).isEqualTo(1);
        assertThat(evictionPolicy.evict()).isEqualTo(2);
        assertThat(evictionPolicy.evict()).isEqualTo(3);
    }

    @Test
    void shouldCleanEvictionPolicyWhenCalledClear() {
        evictionPolicy.onInsert(1);
        evictionPolicy.onInsert(2);
        evictionPolicy.clear();
        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> evictionPolicy.onAccess(1));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");
        illegalStateException = assertThrows(IllegalStateException.class, () -> evictionPolicy.onAccess(2));
        assertThat(illegalStateException.getMessage()).isEqualTo("Missing Key is Accessed in Eviction Policy");

        assertThat(evictionPolicy.evict()).isEqualTo(null);
    }

    @Test
    void shouldIgnoreRemovalOfMissingKey() {

        evictionPolicy.onRemove(100);

        assertThat(evictionPolicy.evict()).isNull();
    }
}
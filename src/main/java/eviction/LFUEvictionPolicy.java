package eviction;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the Least Frequently Used (LFU) eviction policy.
 *
 * <p>This policy evicts the entry that has been accessed the fewest
 * number of times. Ties between entries with the same access frequency
 * are broken by evicting the least recently used entry among them.
 *
 * <p>The implementation maintains a doubly linked list for each access
 * frequency, along with a map from cache keys to their corresponding
 * nodes and a map from access frequencies to their associated linked
 * lists. This organization provides efficient insertion, access updates,
 * removal, and eviction while preserving LFU semantics.
 *
 * <p>Access updates execute in constant time. In certain removal and
 * eviction scenarios, the minimum frequency may be recomputed by
 * scanning the active frequency buckets.
 *
 * @param <K> the type of cache keys
 */
public class LFUEvictionPolicy<K> implements EvictionPolicy<K>{

    /**
     * Node representing a cache key within the LFU eviction policy.
     *
     * <p>Each node stores its associated key, current access frequency,
     * and links used to maintain recency ordering inside a frequency
     * bucket.
     */
    public static class Node<K>{
        private K key;
        private int freq;
        private Node<K> prev;
        private Node<K> next;

        public Node(K key){
            this.key=key;
            this.freq=1;
        }

        public void setPrev(Node<K> prevnode){
            prev=prevnode;
        }

        public void setNext(Node<K> nextnode){
            next=nextnode;
        }

        public K getKey(){
            return key;
        }

        public int getFreq(){
            return freq;
        }

        public void setFreq(int freq){
            this.freq=freq;
        }

        public Node<K> getPrev() {
            return prev;
        }

        public Node<K> getNext(){
            return next;
        }
    }

    /**
     * A doubly linked list with sentinel head and tail nodes used to
     * maintain recency ordering within a single frequency bucket.
     *
     * <p>The least recently used node is located immediately after the
     * head sentinel, while the most recently used node is located
     * immediately before the tail sentinel.
     */
    private static class DLL<K>{
        private final Node<K> head;
        private final Node<K> tail;


        DLL(){
            head=new Node<>(null);
            tail=new Node<>(null);
            head.setNext(tail);
            tail.setPrev(head);
        }

        void addLast(Node<K> node){
            Node<K> tailprev=tail.getPrev();
            tailprev.setNext(node);
            node.setPrev(tailprev);
            node.setNext(tail);
            tail.setPrev(node);
        }

        void remove(Node<K> node){
            Node<K> prevNode=node.getPrev();
            Node<K> nextNode=node.getNext();
            prevNode.setNext(nextNode);
            nextNode.setPrev(prevNode);
            node.setPrev(null);
            node.setNext(null);
        }

        /**
         * Removes and returns the least recently used node from this
         * frequency bucket.
         *
         * @return the least recently used node, or {@code null} if the
         *         bucket is empty
         */
        Node<K> removeFirst(){
            if(head.getNext()==tail){
                return null;
            }
            Node<K> first=head.getNext();
            remove(first);
            return first;
        }

        boolean isEmpty(){
            return head.getNext()==tail;
        }
    }

    private final Map<K, Node<K>> map;
    private final Map<Integer, DLL<K>> freqMap;
    /**
     * The smallest access frequency currently present in the eviction
     * policy. A value of {@code 0} indicates that the policy contains
     * no entries.
     */
    private int minFreq;


    public LFUEvictionPolicy(){
        map=new HashMap<>();
        freqMap=new HashMap<>();
        minFreq=0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void onInsert(K key) {
        if(map.containsKey(key)){
            throw new IllegalStateException(
                    "Duplicate key inserted into eviction policy."
            );
        }
        Node<K> node=new Node<>(key);
        map.put(key,node);
        freqMap.computeIfAbsent(1, f -> new DLL<>()).addLast(node);
        minFreq=1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void onAccess(K key) {
        Node<K> node=map.get(key);
        if(node==null){
            return;
        }
        bumpFreq(node);
    }

    /**
     * Moves a node from its current frequency bucket to the next higher
     * frequency bucket after a successful access.
     *
     * <p>If the previous frequency bucket becomes empty, it is removed.
     * The minimum frequency is updated when necessary.
     *
     * @param node the accessed node whose frequency is to be incremented
     */
    private void bumpFreq(Node<K> node) {
        int oldFreq=node.getFreq();
        DLL<K> oldList=freqMap.get(oldFreq);
        oldList.remove(node);

        if(oldList.isEmpty()){
            freqMap.remove(oldFreq);
            if(minFreq==oldFreq){
                minFreq=oldFreq+1;
            }
        }

        int newFreq=oldFreq+1;
        node.setFreq(newFreq);
        freqMap.computeIfAbsent(newFreq, f -> new DLL<>()).addLast(node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void onRemove(K key) {
        Node<K> node=map.get(key);
        if(node==null){
            return;
        }
        int freq=node.getFreq();
        DLL<K> list=freqMap.get(freq);
        list.remove(node);
        if(list.isEmpty()){
            freqMap.remove(freq);
            if(minFreq==freq){
                minFreq=Integer.MAX_VALUE;
                for(Integer f: freqMap.keySet()){
                    if(f<minFreq){
                        minFreq=f;
                    }
                }
                if(freqMap.isEmpty()){
                    minFreq=0;
                }
            }
        }
        map.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized K evict() {
        if(map.isEmpty()){
            return null;
        }
        DLL<K> list=freqMap.get(minFreq);
        if (list == null) {
            throw new IllegalStateException(
                    "Internal inconsistency: frequency map is out of sync with key map."
            );
        }

        Node<K> first = list.removeFirst();

        if(first == null){
            throw new IllegalStateException(
                    "Internal inconsistency: frequency bucket is empty."
            );
        }

        map.remove(first.getKey());

        if(list.isEmpty()){
            freqMap.remove(minFreq);

            if(freqMap.isEmpty()){
                minFreq = 0;
            }else{
                minFreq = Integer.MAX_VALUE;

                for(Integer f : freqMap.keySet()){
                    minFreq = Math.min(minFreq, f);
                }
            }
        }
        return first.getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        map.clear();
        freqMap.clear();
        minFreq=0;
    }
}
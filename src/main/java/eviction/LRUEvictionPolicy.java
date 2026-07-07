package eviction;

import java.util.HashMap;

public class LRUEvictionPolicy<K> implements EvictionPolicy<K>{


    private final HashMap<K,LRUNode<K>> map;

    private final  LRUNode<K> head;
    private final LRUNode<K> tail;

    public LRUEvictionPolicy(){
        head=new LRUNode<>(null);
        tail=new LRUNode<>(null);
        head.setNext(tail);
        tail.setPrev(head);
        map=new HashMap<>();
    }

    @Override
    public synchronized  void  onInsert(K key) {

        LRUNode<K> node=new LRUNode<>(key);
        addLast(node);
        map.put(key,node);
    }

    @Override
    public synchronized  void onAccess(K key) {

        LRUNode<K> node = map.get(key);
        if(node.getNext()==tail){
            return;
        }
        removeNode(node);
        addLast(node);
    }

    private  void addLast(LRUNode<K> node) {
        node.setNext(null);
        node.setPrev(null);

        LRUNode<K> tailprev=tail.getPrev();
        tailprev.setNext(node);
        node.setPrev(tailprev);
        node.setNext(tail);
        tail.setPrev(node);
    }

    @Override
    public synchronized  void onRemove(K key) {
        LRUNode<K> node=map.get(key);

        removeNode(node);
        map.remove(key);
    }

    private void removeNode(LRUNode<K> node) {


        LRUNode<K> prevNode = node.getPrev();
        LRUNode<K> nextNode = node.getNext();

        prevNode.setNext(nextNode);
        nextNode.setPrev(prevNode);
        node.setPrev(null);
        node.setNext(null);
    }

    @Override
    public synchronized  K evict() {
        return removeFirst();
    }

    private K removeFirst() {
        if(head.getNext()==tail){
            return null;
        }else{
            LRUNode<K> first = head.getNext();

            removeNode(first);

            map.remove(first.getKey());
            return first.getKey();
        }
    }
}

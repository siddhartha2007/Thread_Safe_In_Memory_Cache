package eviction;

import java.util.HashMap;
import java.util.Map;


public class LRUEvictionPolicy<K> implements EvictionPolicy<K>{

    public static class Node<K>{
        private K key;
        private Node<K> prev;
        private Node<K> next;

        public Node(K key){
            this.key=key;
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

        public Node<K> getPrev() {
            return prev;
        }

        public Node<K> getNext(){
            return next;
        }

    }
    private final Map<K, Node<K>> map;

    private final Node<K> head;
    private final Node<K> tail;

    public LRUEvictionPolicy(){
        head=new Node<>(null);
        tail=new Node<>(null);
        head.setNext(tail);
        tail.setPrev(head);
        map=new HashMap<>();
    }

    @Override
    public synchronized  void  onInsert(K key) {

        Node<K> node=new Node<>(key);
        addLast(node);
        map.put(key,node);
    }

    @Override
    public synchronized  void onAccess(K key) {

        Node<K> node = map.get(key);
        if(node==null){
            return;
        }
        if(node.getNext()==tail){
            return;
        }
        removeNode(node);
        addLast(node);
    }

    private  void addLast(Node<K> node) {
        node.setNext(null);
        node.setPrev(null);

        Node<K> tailprev=tail.getPrev();
        tailprev.setNext(node);
        node.setPrev(tailprev);
        node.setNext(tail);
        tail.setPrev(node);
    }

    @Override
    public synchronized  void onRemove(K key) {
        Node<K> node=map.get(key);
        if(node==null){
            return ;
        }
        removeNode(node);
        map.remove(key);
    }

    private void removeNode(Node<K> node) {


        Node<K> prevNode = node.getPrev();
        Node<K> nextNode = node.getNext();

        prevNode.setNext(nextNode);
        nextNode.setPrev(prevNode);
        node.setPrev(null);
        node.setNext(null);
    }

    @Override
    public synchronized  K evict() {
        return removeFirst();
    }

    @Override
    public synchronized  void clear() {
        map.clear();
        head.setNext(tail);
        tail.setPrev(head);
    }

    private K removeFirst() {
        if(head.getNext()==tail){
            return null;
        }else{
            Node<K> first = head.getNext();

            removeNode(first);

            map.remove(first.getKey());
            return first.getKey();
        }
    }
}

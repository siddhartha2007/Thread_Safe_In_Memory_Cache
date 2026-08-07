package eviction;
import java.util.HashMap;

public class FIFOEvictionPolicy<K> implements EvictionPolicy<K>{

    public static class Node<K>{
        private Node<K> next;
        private Node<K> prev;
        private K key;
        public Node(K key){this.key=key;}

        public void setPrev(Node<K> prevNode){
            prev=prevNode;
        }
        public void setNext(Node<K> nextNode){
            next=nextNode;
        }
        public K getKey() {
        return key;
        }

        public Node<K> getPrev(){
            return prev;
        }
        public Node<K> getNext(){
            return next;
        }
    }

    private final HashMap<K,Node<K>> map;
    private final Node<K> head;
    private final Node<K> tail;

    public FIFOEvictionPolicy(){
        map=new HashMap<>();
        head=new Node<>(null);
        tail=new Node<>(null);
        head.setNext(tail);
        tail.setPrev(head);
    }
    @Override
    public synchronized  void  onInsert(K key) {
        if(map.containsKey(key)){
            throw new IllegalStateException( "Duplicate key inserted into eviction policy.");
        }
        Node<K> node=new Node<>(key);
        addLast(node);
        map.put(key,node);
    }

    private void addLast(Node<K> node) {
        node.setPrev(tail.getPrev());
        node.setNext(tail);
        tail.getPrev().setNext(node);
        tail.setPrev(node);
    }

    @Override
    public synchronized  void onAccess(K key) {
        // does nothing.
    }

    @Override
    public synchronized  void onRemove(K key) {

        Node<K> node=map.get(key);
        if(node==null){
            return;
        }
        removeNode(node);
        map.remove(key);
    }

    private void removeNode(Node<K> node) {
        node.getPrev().setNext(node.getNext());
        node.getNext().setPrev(node.getPrev());
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
            Node<K> first = head.getNext();

            removeNode(first);

            map.remove(first.getKey());
            return first.getKey();
        }
    }

    @Override
    public synchronized void clear() {
        map.clear();
        head.setNext(tail);
        tail.setPrev(head);
    }
}

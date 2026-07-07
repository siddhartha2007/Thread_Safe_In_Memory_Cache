package eviction;

public class LRUNode<K>{
    private K key;
    private LRUNode<K> prev;
    private LRUNode<K> next;

   public LRUNode(K key){
       this.key=key;
   }

    public void setPrev(LRUNode<K> prevnode){
        prev=prevnode;
    }

    public void setNext(LRUNode<K> nextnode){
        next=nextnode;
    }

    public K getKey(){
        return key;
    }

    public LRUNode<K> getPrev() {
        return prev;
    }

    public LRUNode<K> getNext(){
        return next;
    }
}

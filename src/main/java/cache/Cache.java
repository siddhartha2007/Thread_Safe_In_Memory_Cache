package cache;

public interface Cache<K,V>{
    void put(K key, V value);

    void put(K key, V value, long ttlMillis);

    V get(K key);

    void remove(K key);

    boolean containsKey(K key);

    int size();

    void clear();

}

// we are using generics so that the cache is reusable for any application.

// for now the put() method is returning void.
// later we can return V so that it returns the old value associated with the key .


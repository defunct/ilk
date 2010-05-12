/**
 * 
 */
package com.goodworkalan.ilk;

import java.util.Collection;
import java.util.Iterator;

public class Collectable implements Collection<Integer> {
    public boolean add(Integer o) {
        return false;
    }

    public boolean addAll(Collection<? extends Integer> c) {
        return false;
    }

    public void clear() {
    }

    public boolean contains(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public Iterator<Integer> iterator() {
        return null;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        return false;
    }

    public int size() {
        return 0;
    }

    public Object[] toArray() {
        return null;
    }

    public <T> T[] toArray(T[] a) {
        return null;
    }
    
}
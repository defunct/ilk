package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.testng.annotations.Test;

public class IlkTest {
    @Test
    public void constructor() {
        Ilk.Key key = new Ilk<Map<String, List<Integer>>>() { }.key;
        Map<Ilk.Key, Integer> map = new HashMap<Ilk.Key, Integer>();
        map.put(key, 1);
        assertEquals((int) map.get(key), 1);
    }
    
    @Test
    public void canContain() {
        Ilk.Key mapKey = new Ilk<Map<String, List<Integer>>>() {}.key;
        assertTrue(mapKey.get(1).getKey().equals(new Ilk<List<Integer>>() { }.key));
        assertFalse(mapKey.get(1).getKey().equals(new Ilk<List<String>>() { }.key));
    }
    
    @Test
    public void assignment() {
        Ilk.Key to = new Ilk<Map<Number, List<String>>>() { }.key;
        Ilk.Key from = new Ilk<TreeMap<Integer, ArrayList<String>>>() { }.key;
        assertTrue(to.isAssignableFrom(from));
        assertFalse(to.isAssignableFrom(new Ilk<TreeMap<Integer, HashSet<String>>>() { }.key));
    }
    
    @Test
    public void box() {
        Ilk.Box box = new Ilk<String>() { }.box("Hello, World!");
        String hello = box.cast(new Ilk<String>() { });
        assertEquals(hello, "Hello, World!");
    }

    @Test
    public void replacement() {
        replace(new Ilk<String>() { });
    }
    
    private <T> void replace(Ilk<T> ilk) {
        Ilk<Map<T, Integer>> mapKey = new Ilk<Map<T, Integer>>(ilk.key) { };
        Ilk.Box pair = mapKey.box(new HashMap<T, Integer>());
        Map<String, Integer> map = pair.cast(new Ilk<Map<String, Integer>>() { });
        assertTrue(map.isEmpty());
    }
    
    @Test
    public void wildcard() {
        Ilk.Box pair = new Ilk<List<Long>>() { }.box(Collections.singletonList(1L));
        assertEquals(pair.cast(new Ilk<List<? super Number>>() { }).size(), 1);
    }
    
    @Test
    public void lookups() throws Exception {
        assertTrue(lookup(new Ilk<One<String>>() { }, new Ilk<Two<String>>() { }, new One<String>()));
        assertTrue(lookup(new Ilk<Collection<String>>() {}, new Ilk<TreeSet<String>>() {}, new ArrayList<String>()));
    }
    
    private <F, T> boolean lookup(Ilk<F> from, Ilk<T> to, F object) throws Exception {
        for (Constructor<T> constructor : to.getConstructors()) {
            if (constructor.getGenericParameterTypes().length == 1) {
                Ilk.Key key;
                try {
                    key = new Ilk.Key(to.key, constructor.getGenericParameterTypes()[0]);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (key.isAssignableFrom(from.key)) {
                    return true;
                }
            }
        }
        return false;
    }
}

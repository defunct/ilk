package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
    /** Test the class constructor. */
    @Test
    public void classConstructor() {
        Ilk<Number> number = new Ilk<Number>(Number.class);
        Ilk<Integer> integer = new Ilk<Integer>(Integer.class);
        assertTrue(number.key.isAssignableFrom(integer.key));
    }
    
    /** Test failed construction. */
    @Test(expectedExceptions = IllegalStateException.class)
    public void tooGeneric() {
        new Ilk<List<String>>();
    }
    
    /** Test to string. */
    @Test
    public void string() {
        assertEquals(new Ilk<List<String>>() {}.key.toString(), "java.util.List<java.lang.String>");
    }
    
    /** Test copy constructor. */
    @Test
    public void copy() {
        Ilk.Key key = new Ilk.Key(new Ilk<List<String>>() {}.key);
        assertEquals(key.toString(),  "java.util.List<java.lang.String>");
    }
    
    /** Test parameter get. */
    @Test
    public void get() {
        Ilk.Key key = new Ilk.Key(new Ilk<List<String>>() {}.key);
        assertNotNull(key.get("E"));
        assertNull(key.get("Z"));
    }

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
        assertTrue(mapKey.parameters.get(1).key.isAssignableFrom(new Ilk<List<Integer>>() { }.key));
        assertFalse(mapKey.parameters.get(1).key.isAssignableFrom(new Ilk<List<String>>() { }.key));
    }
    
    @Test
    public void assignment() {
        Ilk.Key to = new Ilk<Map<? extends Number, ? extends List<String>>>() { }.key;
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
        Ilk.Box box = mapKey.box(new HashMap<T, Integer>());
        Map<String, Integer> map = box.cast(new Ilk<Map<String, Integer>>() { });
        assertTrue(map.isEmpty());
    }
    
    @Test
    public void wildcard() {
        Ilk.Box box = new Ilk<List<Long>>() { }.box(Collections.singletonList(1L));
        assertEquals(box.cast(new Ilk<List<? extends Number>>() { }).size(), 1);
        box = new Ilk<List<Number>>() { }.box(Collections.<Number>singletonList(1L));
        assertEquals(box.cast(new Ilk<List<? super Number>>() { }).size(), 1);
    }
    
    @Test(expectedExceptions = ClassCastException.class)
    public void wildcardLowerBounds() {
        Ilk.Box box = new Ilk<List<Long>>() { }.box(Collections.singletonList(1L));
        box.cast(new Ilk<List<? super Number>>() { });
    }
    
    public static class Foo<T extends Number> {
    }
    
    @Test
    public void foo() {
        // FIXME Uh, oh. More to play with.
        TypeVariable<?> tv = Foo.class.getTypeParameters()[0];
        for (Type bound : tv.getBounds()) {
            System.out.println(bound);
        }
        System.out.println(tv);
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

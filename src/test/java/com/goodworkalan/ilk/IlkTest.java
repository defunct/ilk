package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.testng.annotations.Test;

/**
 * Unit tests for the {@link IlkReflect} class.
 *
 * @author Alan Gutierrez
 */
public class IlkTest {
    /** Test the class constructor. */
    @Test
    public void classConstructor() {
        IlkReflect<Number> number = new IlkReflect<Number>(Number.class);
        IlkReflect<Integer> integer = new IlkReflect<Integer>(Integer.class);
        assertTrue(number.key.isAssignableFrom(integer.key));
    }

    /** Test failed construction. */
    @Test(expectedExceptions = IllegalStateException.class)
    public void tooGeneric() {
        new IlkReflect<List<String>>();
    }
    
    /** Test to string. */
    @Test
    public void string() {
        assertEquals(new IlkReflect<List<String>>() {}.key.toString(), "java.util.List<java.lang.String>");
        assertEquals(new IlkReflect<String>() {}.key.toString(), "java.lang.String");
    }
    
    /** Test copy constructor. */
    @Test
    public void copy() {
        IlkReflect.Key key = new IlkReflect.Key(new IlkReflect<List<String>>() {}.key);
        assertEquals(key.toString(),  "java.util.List<java.lang.String>");
    }
    
    /** Test quality. */
    @Test
    public void equality() {
        IlkReflect.Key key = new IlkReflect.Key(String.class);
        assertEquals(key, key);
        assertEquals(key, new IlkReflect.Key(String.class));
        assertFalse(key.equals(new IlkReflect.Key(Integer.class)));
        IlkReflect.Key listString = new IlkReflect<List<String>>() {}.key;
        assertFalse(key.equals(listString));
        assertFalse(listString.equals(key));
        assertEquals(listString, listString);
        assertFalse(listString.equals(new IlkReflect<List<Integer>>(){}.key));
        ParameterizedType pt = (ParameterizedType) listString.type;
        assertEquals(listString, new IlkReflect.Key(new Types.ParameterizedType(pt, pt.getActualTypeArguments())));
        assertFalse(listString.equals(new IlkReflect<Collection<String>>() { }.key));
        assertFalse(new IlkReflect<Three<String>.Four<Integer>>() { }.key.equals(new IlkReflect<Three<Integer>.Four<Integer>>() { }.key));
        assertFalse(listString.equals(null));
    }

    /** Test hash code. */
    @Test
    public void hash() {
        IlkReflect.Key key = new IlkReflect<Map<String, List<Integer>>>() { }.key;
        Map<IlkReflect.Key, Integer> map = new HashMap<IlkReflect.Key, Integer>();
        map.put(key, 1);
        assertEquals((int) map.get(key), 1);
    }
    
    /** Test super key. */
    @Test
    public void getSuperKey() {
        IlkReflect<FooMap<ArrayList<String>, Integer>> ilk = new IlkReflect<FooMap<ArrayList<String>, Integer>>(){};
        IlkReflect.Key mapKey = ilk.key.getSuperKey(SortedMap.class);
        assertEquals(mapKey.toString(), "java.util.SortedMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        mapKey = mapKey.getSuperKey(Map.class);
        assertEquals(mapKey.toString(), "java.util.Map<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        assertEquals(ilk.key.getSuperKey(Serializable.class).toString(), "java.io.Serializable");
        assertEquals(ilk.key.getSuperKey(Object.class).toString(), "java.lang.Object");
        assertEquals(ilk.key.getSuperKey(TreeMap.class).toString(), "java.util.TreeMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        IlkReflect.Key collectable = new IlkReflect<Collectable>() { }.key;
        assertEquals(collectable.getSuperKey(Collection.class).toString(), "java.util.Collection<java.lang.Integer>");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void badSuperKey() {
        IlkReflect<FooMap<ArrayList<String>, Integer>> ilk = new IlkReflect<FooMap<ArrayList<String>, Integer>>(){};
        assertNull(ilk.key.getSuperKey(Number.class));
    }

    /** Test is assignable from. */
    @Test
    public void assignment() {
        IlkReflect.Key string = new IlkReflect<String>() { }.key;
        IlkReflect.Key number = new IlkReflect<Number>() { }.key;
        IlkReflect.Key integer = new IlkReflect<Integer>() { }.key;
        assertFalse(number.isAssignableFrom(string));
        assertTrue(number.isAssignableFrom(integer));
        IlkReflect.Key listString = new IlkReflect<List<String>>() { }.key;
        IlkReflect.Key arrayListString = new IlkReflect<ArrayList<String>>() { }.key;
        assertTrue(listString.isAssignableFrom(arrayListString));
        assertFalse(listString.isAssignableFrom(new IlkReflect<List<Integer>>() { }.key));
        IlkReflect.Key listMapStringInteger = new IlkReflect<List<Map<String, Integer>>>() { }.key;
        
        // Test non-wildcards.
        // List<TreeMap<String, Integer>> bar = new ArrayList<TreeMap<String,Integer>>();
        // List<Map<String, Integer>> foo =  bar; <!-- DOES NOT WORK!
        
        assertFalse(listMapStringInteger.isAssignableFrom(listString));
        IlkReflect.Key listTreeMapStringInteger = new IlkReflect<List<TreeMap<String, Integer>>>() { }.key;
        assertFalse(listMapStringInteger.isAssignableFrom(listTreeMapStringInteger));
        assertTrue(listMapStringInteger.isAssignableFrom(listMapStringInteger));
        IlkReflect.Key listExtendsNumber = new IlkReflect<List<? extends Number>>() { }.key;
        IlkReflect.Key listInteger = new IlkReflect<List<Integer>>() { }.key;
        assertTrue(listExtendsNumber.isAssignableFrom(listInteger));
        assertFalse(listExtendsNumber.isAssignableFrom(listString));
        IlkReflect.Key listNumber = new IlkReflect<List<Number>>() { }.key;
        IlkReflect.Key listSuperInteger = new IlkReflect<List<? super Integer>>() { }.key;
        assertTrue(listSuperInteger.isAssignableFrom(listNumber));
        assertFalse(listSuperInteger.isAssignableFrom(listString));
        IlkReflect.Key listExtendsListSuperListString = new IlkReflect<List<? extends List<? super List<String>>>>() { }.key;
        IlkReflect.Key arrayListArrayListSuperListString = new IlkReflect<ArrayList<ArrayList<? super List<String>>>>() {}.key;
        
        // Test wildcards.
        // List<? extends List<? super List<String>>> list = new ArrayList<ArrayList<? super List<String>>>();
        
        assertTrue(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListSuperListString));
        IlkReflect.Key arrayListArrayListListString = new IlkReflect<ArrayList<ArrayList<List<String>>>>() {}.key;
        assertFalse(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListListString));
        assertTrue(listExtendsListSuperListString.isAssignableFrom(listExtendsListSuperListString));
        IlkReflect.Key arrayListArrayListSuperSetString = new IlkReflect<ArrayList<ArrayList<? super Set<String>>>>() {}.key;
        assertFalse(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListSuperSetString));
        IlkReflect.Key arrayListArrayListExtendsSetString = new IlkReflect<ArrayList<ArrayList<? extends Set<String>>>>() {}.key;
        IlkReflect.Key arrayListArrayListExtendsListString = new IlkReflect<ArrayList<ArrayList<? extends List<String>>>>() {}.key;
        assertFalse(arrayListArrayListExtendsListString.isAssignableFrom(arrayListArrayListExtendsSetString));
    }
    
    /** Test key ordering. */
    @Test
    public void sort() {
        IlkReflect.Key string = new IlkReflect<String>() { }.key;
        IlkReflect.Key number = new IlkReflect<Number>() { }.key;
        IlkReflect.Key integer = new IlkReflect<Integer>() { }.key;
        IlkReflect.Key listString = new IlkReflect<List<String>>() { }.key;
        IlkReflect.Key arrayListString = new IlkReflect<ArrayList<String>>() { }.key;
        IlkReflect.Key listMapStringInteger = new IlkReflect<List<Map<String, Integer>>>() { }.key;
        IlkReflect.Key listTreeMapStringInteger = new IlkReflect<List<TreeMap<String, Integer>>>() { }.key;
        IlkReflect.Key listExtendsNumber = new IlkReflect<List<? extends Number>>() { }.key;
        IlkReflect.Key listInteger = new IlkReflect<List<Integer>>() { }.key;
        IlkReflect.Key listLong = new IlkReflect<List<Long>>() { }.key;
        assertTrue(string.compareTo(string) == 0);
        assertTrue(string.compareTo(number) > 0);
        assertTrue(number.compareTo(integer) > 0);
        assertTrue(integer.compareTo(number) < 0);
        Set<IlkReflect.Key> keys = new TreeSet<IlkReflect.Key>();
        keys.add(string);
        keys.add(number);
        keys.add(integer);
        keys.add(listString);
        keys.add(arrayListString);
        keys.add(listMapStringInteger);
        keys.add(listTreeMapStringInteger);
        keys.add(listExtendsNumber);
        keys.add(listInteger);
        for (IlkReflect.Key candidate : keys) {
            if (candidate.isAssignableFrom(arrayListString)) {
                assertEquals(candidate, arrayListString);
                break;
            }
        }
        for (IlkReflect.Key candidate : keys) {
            if (candidate.isAssignableFrom(listLong)) {
                assertEquals(candidate, listExtendsNumber);
                break;
            }
        }
    }
    
    /** Test box. */
    @Test
    public void box() {
        IlkReflect.Box box = new IlkReflect<String>() { }.box("Hello, World!");
        String hello = box.cast(String.class);
        assertEquals(box.key, new IlkReflect<String>() { }.key);
        assertEquals(box.object, "Hello, World!");
        assertEquals(hello, "Hello, World!");
        box = new IlkReflect<String>(String.class).box("Hello, World!");
        assertEquals(box.key, new IlkReflect<String>() { }.key);
        assertEquals(box.object, "Hello, World!");
        assertEquals(hello, "Hello, World!");
    }
    
    /** Test a bad box cast. */
    @Test(expectedExceptions = ClassCastException.class)
    public void boxBadCast() {
        IlkReflect.Box box = new IlkReflect<String>() { }.box("Hello, World!");
        box.cast(Integer.class);
    }

    /** Test type variable replacement. */
    @Test
    public void replacement() {
        IlkReflect<Map<String, Integer>> map = replace(new IlkReflect<String>() { });
        IlkReflect.Box box = map.box(new HashMap<String, Integer>());
        box.cast(new IlkReflect<Map<String, Integer>>() { });
    }

    /** Test variables with bounds. */
    private <T extends CharSequence & Serializable> IlkReflect<Map<T, Integer>> replace(IlkReflect<T> ilk) {
        return new IlkReflect<Map<T, Integer>>(ilk.key) { };
    }
    
    /** Test key replacement. */
    @Test
    public void keyReplace() {
        IlkReflect.Key string = new IlkReflect<String>() {}.key;
        IlkReflect.Key bar = new IlkReflect<Bar<?>>() { }.key;
        new IlkReflect.Key((ParameterizedType) bar.type, string);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void badKeyReplace() {
        IlkReflect.Key integer = new IlkReflect<Integer>() {}.key;
        IlkReflect.Key bar = new IlkReflect<Bar<?>>() { }.key;
        new IlkReflect.Key((ParameterizedType) bar.type, integer);
    }
    
    @Test
    public void classBox() {
        IlkReflect<Class<Integer>> classIlk = new IlkReflect<Class<Integer>>() { };
        IlkReflect.Box classBox = new IlkReflect.Box(new Integer(0).getClass());
        Class<Integer> intClass = classBox.cast(classIlk);
        assertEquals(intClass, Integer.class);
    }
}

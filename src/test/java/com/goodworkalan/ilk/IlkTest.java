package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
        assertEquals(new Ilk<String>() {}.key.toString(), "java.lang.String");
    }
    
    /** Test copy constructor. */
    @Test
    public void copy() {
        Ilk.Key key = new Ilk.Key(new Ilk<List<String>>() {}.key);
        assertEquals(key.toString(),  "java.util.List<java.lang.String>");
    }
    
    /** Test the constructor iterator.  */
    @Test
    public void constructors() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Ilk<ArrayList<String>> ilk = new Ilk<ArrayList<String>>() { };
        List<String> list = null;
        for (Constructor<ArrayList<String>> constructor : ilk.getConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                list = constructor.newInstance();
            }
        }
        assertNotNull(list);
    }
    
    /** Test quality. */
    @Test
    public void equality() {
        Ilk.Key key = new Ilk.Key(String.class);
        assertEquals(key, key);
        assertEquals(key, new Ilk.Key(String.class));
        assertFalse(key.equals(new Ilk.Key(Integer.class)));
        Ilk.Key listString = new Ilk<List<String>>() {}.key;
        assertFalse(key.equals(listString));
        assertFalse(listString.equals(key));
        assertEquals(listString, listString);
        assertFalse(listString.equals(new Ilk<List<Integer>>(){}.key));
        ParameterizedType pt = (ParameterizedType) listString.type;
        assertEquals(listString, new Ilk.Key(new Types.ParameterizedType(pt, pt.getActualTypeArguments())));
        assertFalse(listString.equals(new Ilk<Collection<String>>() { }.key));
        assertFalse(new Ilk<Three<String>.Four<Integer>>() { }.key.equals(new Ilk<Three<Integer>.Four<Integer>>() { }.key));
        assertFalse(listString.equals(null));
    }

    /** Test hash code. */
    @Test
    public void hash() {
        Ilk.Key key = new Ilk<Map<String, List<Integer>>>() { }.key;
        Map<Ilk.Key, Integer> map = new HashMap<Ilk.Key, Integer>();
        map.put(key, 1);
        assertEquals((int) map.get(key), 1);
    }
    
    /** Test super key. */
    @Test
    public void getSuperKey() {
        Ilk<FooMap<ArrayList<String>, Integer>> ilk = new Ilk<FooMap<ArrayList<String>, Integer>>(){};
        Ilk.Key mapKey = ilk.key.getSuperKey(SortedMap.class);
        assertEquals(mapKey.toString(), "java.util.SortedMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        mapKey = mapKey.getSuperKey(Map.class);
        assertEquals(mapKey.toString(), "java.util.Map<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        assertEquals(ilk.key.getSuperKey(Serializable.class).toString(), "java.io.Serializable");
        assertEquals(ilk.key.getSuperKey(Object.class).toString(), "java.lang.Object");
        assertEquals(ilk.key.getSuperKey(TreeMap.class).toString(), "java.util.TreeMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        assertNull(ilk.key.getSuperKey(Number.class));
        assertNull(ilk.key.getSuperKey(CharSequence.class));
        Ilk.Key collectable = new Ilk<Collectable>() { }.key;
        assertEquals(collectable.getSuperKey(Collection.class).toString(), "java.util.Collection<java.lang.Integer>");
    }

    /** Test is assignable from. */
    @Test
    public void assignment() {
        Ilk.Key string = new Ilk<String>() { }.key;
        Ilk.Key number = new Ilk<Number>() { }.key;
        Ilk.Key integer = new Ilk<Integer>() { }.key;
        assertFalse(number.isAssignableFrom(string));
        assertTrue(number.isAssignableFrom(integer));
        Ilk.Key listString = new Ilk<List<String>>() { }.key;
        Ilk.Key arrayListString = new Ilk<ArrayList<String>>() { }.key;
        assertTrue(listString.isAssignableFrom(arrayListString));
        assertFalse(listString.isAssignableFrom(new Ilk<List<Integer>>() { }.key));
        Ilk.Key listMapStringInteger = new Ilk<List<Map<String, Integer>>>() { }.key;
        
        // Test non-wildcards.
        // List<TreeMap<String, Integer>> bar = new ArrayList<TreeMap<String,Integer>>();
        // List<Map<String, Integer>> foo =  bar; <!-- DOES NOT WORK!
        
        assertFalse(listMapStringInteger.isAssignableFrom(listString));
        Ilk.Key listTreeMapStringInteger = new Ilk<List<TreeMap<String, Integer>>>() { }.key;
        assertFalse(listMapStringInteger.isAssignableFrom(listTreeMapStringInteger));
        assertTrue(listMapStringInteger.isAssignableFrom(listMapStringInteger));
        Ilk.Key listExtendsNumber = new Ilk<List<? extends Number>>() { }.key;
        Ilk.Key listInteger = new Ilk<List<Integer>>() { }.key;
        assertTrue(listExtendsNumber.isAssignableFrom(listInteger));
        assertFalse(listExtendsNumber.isAssignableFrom(listString));
        Ilk.Key listNumber = new Ilk<List<Number>>() { }.key;
        Ilk.Key listSuperInteger = new Ilk<List<? super Integer>>() { }.key;
        assertTrue(listSuperInteger.isAssignableFrom(listNumber));
        assertFalse(listSuperInteger.isAssignableFrom(listString));
        Ilk.Key listExtendsListSuperListString = new Ilk<List<? extends List<? super List<String>>>>() { }.key;
        Ilk.Key arrayListArrayListSuperListString = new Ilk<ArrayList<ArrayList<? super List<String>>>>() {}.key;
        
        // Test wildcards.
        // List<? extends List<? super List<String>>> list = new ArrayList<ArrayList<? super List<String>>>();
        
        assertTrue(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListSuperListString));
        Ilk.Key arrayListArrayListListString = new Ilk<ArrayList<ArrayList<List<String>>>>() {}.key;
        assertFalse(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListListString));
        assertTrue(listExtendsListSuperListString.isAssignableFrom(listExtendsListSuperListString));
        Ilk.Key arrayListArrayListSuperSetString = new Ilk<ArrayList<ArrayList<? super Set<String>>>>() {}.key;
        assertFalse(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListSuperSetString));
        Ilk.Key arrayListArrayListExtendsSetString = new Ilk<ArrayList<ArrayList<? extends Set<String>>>>() {}.key;
        Ilk.Key arrayListArrayListExtendsListString = new Ilk<ArrayList<ArrayList<? extends List<String>>>>() {}.key;
        assertFalse(arrayListArrayListExtendsListString.isAssignableFrom(arrayListArrayListExtendsSetString));
    }
    
    /** Test key ordering. */
    @Test
    public void sort() {
        Ilk.Key string = new Ilk<String>() { }.key;
        Ilk.Key number = new Ilk<Number>() { }.key;
        Ilk.Key integer = new Ilk<Integer>() { }.key;
        Ilk.Key listString = new Ilk<List<String>>() { }.key;
        Ilk.Key arrayListString = new Ilk<ArrayList<String>>() { }.key;
        Ilk.Key listMapStringInteger = new Ilk<List<Map<String, Integer>>>() { }.key;
        Ilk.Key listTreeMapStringInteger = new Ilk<List<TreeMap<String, Integer>>>() { }.key;
        Ilk.Key listExtendsNumber = new Ilk<List<? extends Number>>() { }.key;
        Ilk.Key listInteger = new Ilk<List<Integer>>() { }.key;
        Ilk.Key listLong = new Ilk<List<Long>>() { }.key;
        assertTrue(string.compareTo(string) == 0);
        assertTrue(string.compareTo(number) > 0);
        assertTrue(number.compareTo(integer) > 0);
        assertTrue(integer.compareTo(number) < 0);
        Set<Ilk.Key> keys = new TreeSet<Ilk.Key>();
        keys.add(string);
        keys.add(number);
        keys.add(integer);
        keys.add(listString);
        keys.add(arrayListString);
        keys.add(listMapStringInteger);
        keys.add(listTreeMapStringInteger);
        keys.add(listExtendsNumber);
        keys.add(listInteger);
        for (Ilk.Key candidate : keys) {
            if (candidate.isAssignableFrom(arrayListString)) {
                assertEquals(candidate, arrayListString);
                break;
            }
        }
        for (Ilk.Key candidate : keys) {
            if (candidate.isAssignableFrom(listLong)) {
                assertEquals(candidate, listExtendsNumber);
                break;
            }
        }
    }
    
    /** Test box. */
    @Test
    public void box() {
        Ilk.Box box = new Ilk<String>() { }.box("Hello, World!");
        String hello = box.cast(String.class);
        assertEquals(box.key, new Ilk<String>() { }.key);
        assertEquals(box.object, "Hello, World!");
        assertEquals(hello, "Hello, World!");
        box = new Ilk<String>(String.class).box("Hello, World!");
        assertEquals(box.key, new Ilk<String>() { }.key);
        assertEquals(box.object, "Hello, World!");
        assertEquals(hello, "Hello, World!");
    }
    
    /** Test a bad box cast. */
    @Test(expectedExceptions = ClassCastException.class)
    public void boxBadCast() {
        Ilk.Box box = new Ilk<String>() { }.box("Hello, World!");
        box.cast(Integer.class);
    }

    @Test
    public void replacement() {
        replace(new Ilk<String>() { });
    }
    
    private <T extends CharSequence & Serializable> void replace(Ilk<T> ilk) {
        Ilk<Map<T, Integer>> mapKey = new Ilk<Map<T, Integer>>() { };
//        Ilk<Map<T, Integer>> mapKey = new Ilk<Map<T, Integer>>(ilk.key) { };
//        Ilk.Box box = mapKey.box(new HashMap<T, Integer>());
//        Map<String, Integer> map = box.cast(new Ilk<Map<String, Integer>>() { });
//        assertTrue(map.isEmpty());
        TypeVariable<?> a = (TypeVariable<?>) ((ParameterizedType) mapKey.key.type).getActualTypeArguments()[0];
        Method method = (Method) a.getGenericDeclaration();
        TypeVariable<?> t = (TypeVariable<?>) ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
        for (Type bound : t.getBounds()) {
            System.out.println(bound);
        }
        System.out.println(method.getGenericParameterTypes()[0]);
    }
    
    public static class Foo<T extends Number> {
    }
    
    @Test(enabled = false)
    public void foo() {
        // FIXME Uh, oh. More to play with.
        TypeVariable<?> tv = Foo.class.getTypeParameters()[0];
        for (Type bound : tv.getBounds()) {
            System.out.println(bound);
        }
        System.out.println(tv);
    }
    
    // Here was an example of a possible use of variable substitution, finding a constructor
    // that would convert form one type to the next.
//    @Test(enabled = false)
//    public void lookups() throws Exception {
//        assertTrue(lookup(new Ilk<One<String>>() { }, new Ilk<Two<String>>() { }, new One<String>()));
//        assertTrue(lookup(new Ilk<Collection<String>>() {}, new Ilk<TreeSet<String>>() {}, new ArrayList<String>()));
//    }
//    
//    private <F, T> boolean lookup(Ilk<F> from, Ilk<T> to, F object) throws Exception {
//        for (Constructor<T> constructor : to.getConstructors()) {
//            if (constructor.getGenericParameterTypes().length == 1) {
//                Ilk.Key key;
//                try {
//                    key = null; //new Ilk.Key(to.key, constructor.getGenericParameterTypes()[0]);
//                } catch (IllegalArgumentException e) {
//                    continue;
//                }
//                if (key.isAssignableFrom(from.key)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}

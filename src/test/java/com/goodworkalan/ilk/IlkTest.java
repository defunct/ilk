package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.testng.annotations.Test;

/**
 * Unit tests for the {@link Ilk} class.
 *
 * @author Alan Gutierrez
 */
public class IlkTest {
    /** Test the class constructor. */
    @Test
    public void classConstructor() {
        Ilk<Number> number = new Ilk<Number>(Number.class);
        Ilk<Integer> integer = new Ilk<Integer>(Integer.class);
        assertTrue(number.key.isAssignableFrom(integer.key));
    }

    /** Test failed construction. */
    @Test(expectedExceptions = ClassCastException.class)
    public void tooGeneric() {
        new Ilk<List<String>>();
    }
    
    /** Test to string. */
    @Test
    public void string() {
        assertEquals(new Ilk<List<String>>() {}.key.toString(), "java.util.List<java.lang.String>");
        assertEquals(new Ilk<String>() {}.key.toString(), "java.lang.String");
    }
    
//    /** Test copy constructor. */
//    @Test
//    public void copy() {
//        Ilk.Key key = new Ilk.Key(new Ilk<List<String>>() {}.key);
//        assertEquals(key.toString(),  "java.util.List<java.lang.String>");
//    }
    
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
        assertEquals(listString, new Ilk.Key(new Types.Parameterized(pt.getRawType(), pt.getOwnerType(), pt.getActualTypeArguments())));
        assertFalse(listString.equals(new Ilk<Collection<String>>() { }.key));
        assertFalse(new Ilk<Three<String>.Four<Integer>>() { }.key.equals(new Ilk<Three<Integer>.Four<Integer>>() { }.key));
        assertFalse(listString.equals(null));
    }
    
    @Test
    public void actualTypes() throws Exception {
        Ilk.Key key = new Ilk<TreeMap<String, List<Integer>>>() { }.key;
        Type actualMapType = Types.getActualType(Map.class, key.type);
        Method put = Types.getRawClass(actualMapType).getMethod("put", Object.class, Object.class);
        Type[] generics = put.getGenericParameterTypes();
        for (int i = 0; i < generics.length; i++) {
            System.out.println(Types.getActualType(generics[i], key.type));
        }
    }

    /** Test hash code. */
    @Test
    public void hash() {
        Ilk.Key key = new Ilk<Map<String, List<Integer>>>() { }.key;
        Map<Ilk.Key, Integer> map = new HashMap<Ilk.Key, Integer>();
        map.put(key, 1);
        assertEquals((int) map.get(key), 1);
    }
    
    public Ilk.Key getSuperKey(Ilk.Key key, Class<?> keyClass) {
      return new Ilk.Key(Types.getActualType(keyClass, key.type));
  }

    @Test
    public void boxAClass() {
        Ilk.Box box = new Ilk.Box(new Ilk<Class<Object>>() {});
        assertTrue(box.key.type instanceof ParameterizedType);
    }
    
    @Test
    public void ilkBox() {
        Ilk.Box box = new Ilk<List<String>>() {}.box();
        Ilk<List<String>> unboxed = box.cast(new Ilk<Ilk<List<String>>>() {});
        System.out.println(unboxed);
    }
    
    @Test <T> void ilkFromKey() {
        Ilk.Box box = new Ilk<List<T>>() {}.assign(new Ilk<T>() {}, String.class).box();
        Ilk<List<String>> ilkString = box.cast(new Ilk<Ilk<List<String>>>() {});
        System.out.println(ilkString);
    }
    
    @Test
    public  void assign() {
        Ilk.Box box = assignMap(new Ilk<Integer>(Integer.class), new Ilk<String>(String.class), new ArrayList<Map<Integer, String>>());
        List<Map<Integer, String>> listMap = box.cast(new Ilk<List<Map<Integer, String>>>() {});
        System.out.println(listMap);
    }
    
    public <K, V> Ilk.Box assignMap(Ilk<K> k, Ilk<V> v, List<Map<K, V>> unboxed) {
        Ilk<List<Map<K, V>>> listMap = new Ilk<List<Map<K,V>>>(){};
        System.out.println(listMap);
        Ilk<List<Map<K, V>>> assignK = listMap.assign(new Ilk<Ilk<K>>() {}, k);
        Ilk<List<Map<K, V>>> assignV = assignK.assign(new Ilk<Ilk<V>>() {}, v);
        return assignV.box(unboxed);
    }
    
    /** Test super key. */
    @Test
    public void getSuperKey() {
        Ilk<FooMap<ArrayList<String>, Integer>> ilk = new Ilk<FooMap<ArrayList<String>, Integer>>(){};
        Ilk.Key mapKey = getSuperKey(ilk.key, SortedMap.class);
        assertEquals(mapKey.toString(), "java.util.SortedMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        mapKey = getSuperKey(mapKey, Map.class);
        assertEquals(mapKey.toString(), "java.util.Map<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        assertEquals(getSuperKey(ilk.key, Serializable.class).toString(), "java.io.Serializable");
        assertEquals(getSuperKey(ilk.key, Object.class).toString(), "java.lang.Object");
        assertEquals(getSuperKey(ilk.key, TreeMap.class).toString(), "java.util.TreeMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>");
        Ilk.Key collectable = new Ilk<Collectable>() { }.key;
        assertEquals(getSuperKey(collectable, Collection.class).toString(), "java.util.Collection<java.lang.Integer>");
    }
    
//    @Test(expectedExceptions = IllegalArgumentException.class)
//    public void badSuperKey() {
//        Ilk<FooMap<ArrayList<String>, Integer>> ilk = new Ilk<FooMap<ArrayList<String>, Integer>>(){};
//        assertNull(getSuperKey(ilk.key, Number.class));
//    }

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
//         List<? extends List<? super List<String>>> list = new ArrayList<ArrayList<? super List<String>>>();
        
        assertTrue(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListSuperListString));
        Ilk.Key arrayListArrayListListString = new Ilk<ArrayList<ArrayList<List<String>>>>() {}.key;
        assertFalse(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListListString));
        assertTrue(listExtendsListSuperListString.isAssignableFrom(listExtendsListSuperListString));
        Ilk.Key arrayListArrayListSuperSetString = new Ilk<ArrayList<ArrayList<? super Set<String>>>>() {}.key;
        assertFalse(listExtendsListSuperListString.isAssignableFrom(arrayListArrayListSuperSetString));
        Ilk.Key arrayListArrayListExtendsSetString = new Ilk<ArrayList<ArrayList<? extends Set<String>>>>() {}.key;
        Ilk.Key arrayListArrayListExtendsListString = new Ilk<ArrayList<ArrayList<? extends List<String>>>>() {}.key;
        assertFalse(arrayListArrayListExtendsListString.isAssignableFrom(arrayListArrayListExtendsSetString));
        
        Ilk.Key superWildExtendsString = new Ilk<SuperWild<? extends String>>() {}.key;
        Ilk.Key superWildString = new Ilk<SuperWild<String>>() {}.key;
//        
//        // SuperWild<? extends String> sw = new SuperWild<String>();
        assertTrue(superWildExtendsString.isAssignableFrom(superWildString));
        assertFalse(superWildString.isAssignableFrom(superWildExtendsString));
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
    public void genericType() throws SecurityException, NoSuchFieldException {
        Ilk<Four> ilk = new Ilk<Four>(){};
        Type type = Types.getActualType(Four.class.getField("strings").getGenericType(), ilk.key.type);
        assertEquals(type.toString(), "java.util.List<java.lang.String>");
    }

//    @Test
//    public void classBox() {
//        Ilk<Class<Integer>> classIlk = new Ilk<Class<Integer>>() { };
//        Ilk.Box classBox = new Ilk.Box(new Integer(0).getClass());
//        Class<Integer> intClass = classBox.cast(classIlk);
//        assertEquals(intClass, Integer.class);
//    }
}

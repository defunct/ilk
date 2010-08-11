package com.goodworkalan.ilk;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
        assertEquals(Number.class, number.key.type);
    }

    /** Test failed super type token constructor. */
    @Test(expectedExceptions = ClassCastException.class)
    public void noSuperType() {
        new Ilk<List<String>>();
    }
    
    /** Keys can correctly determine if a class is assignable to another class. */
    @Test
    public void classAssignable() {
        Ilk<Number> number = new Ilk<Number>(Number.class);
        Ilk<Integer> integer = new Ilk<Integer>(Integer.class);
        assertTrue(number.key.isAssignableFrom(integer.key));
        Ilk<String> string = new Ilk<String>(String.class);
        assertFalse(number.key.isAssignableFrom(string.key));
    }
    
    /** Two keys of the same class are equal. */
    @Test
    public void classKeyEquality() {
        Ilk.Key key = new Ilk.Key(String.class);
        assertEquals(key, new Ilk.Key(String.class));
        assertFalse(key.equals(new Ilk.Key(Number.class)));
    }
    
    /** An instance of a key is equal to itself. */
    @Test
    public void classSame() {
        Ilk.Key key = new Ilk.Key(String.class);
        assertEquals(key, key);
    }
    
    /** Construct a super type token. */
    @Test
    public void constructSuperTypeToken() {
        Ilk<List<String>> listString = new Ilk<List<String>>() {};
        assertTrue(ParameterizedType.class.isAssignableFrom(listString.key.type.getClass()));
        ParameterizedType pt = (ParameterizedType) listString.key.type;
        assertEquals(List.class, pt.getRawType());
        assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }
    
    /** A super type token is equal to a super type token of the same type. */
    @Test
    public void superTypeTokenKeyEquality() {
        Ilk<List<String>> listString = new Ilk<List<String>>() {};
        assertEquals(listString.key, new Ilk<List<String>>() {}.key);
        assertFalse(listString.key.equals(new Ilk<Set<String>>() {}.key));
        assertFalse(listString.key.equals(new Ilk<List<Number>>() {}.key));
    }
    
    /** A super type token is equal to itself. */
    @Test
    public void superTypeTokenSame() {
        Ilk<List<String>> listString = new Ilk<List<String>>() {};
        assertEquals(listString, listString);
    }

    /** Test quality. */
    @Test(enabled = false)
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
    
    /** Test assigning actual generic type parameters. */
    @Test
    public void actualTypes() throws Exception {
        Ilk.Key key = new Ilk<TreeMap<String, List<Integer>>>() { }.key;
        Type actualMapType = Types.getActualType(Map.class, key.type, new LinkedList<Map<TypeVariable<?>, Type>>());
        Method put = Types.getRawClass(actualMapType).getMethod("put", Object.class, Object.class);
        Type[] generics = put.getGenericParameterTypes();
        for (int i = 0; i < generics.length; i++) {
            System.out.println(Types.getActualType(generics[i], key.type, new LinkedList<Map<TypeVariable<?>, Type>>()));
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
    
    /** Test to string. */
    @Test
    public void string() {
        assertEquals(new Ilk<List<String>>() {}.key.toString(), "java.util.List<java.lang.String>");
        assertEquals(new Ilk<String>() {}.key.toString(), "java.lang.String");
    }

    public Ilk.Key getSuperKey(Ilk.Key key, Class<?> keyClass) {
      return new Ilk.Key(Types.getActualType(keyClass, key.type, new LinkedList<Map<TypeVariable<?>, Type>>()));
  }

    /** Test creating a box that contains a class. */
    @Test
    public void boxAClass() {
        Ilk.Box box = new Ilk.Box(new Ilk<Class<Object>>() {});
        assertTrue(box.key.type instanceof ParameterizedType);
    }
    
    /** Test box. */
    @Test
    public void ilkBox() {
        Ilk.Box box = new Ilk.Box(new Ilk<List<String>>() {}.key.type);
        Ilk<List<String>> unboxed = box.cast(new Ilk<Ilk<List<String>>>() {});
        System.out.println(unboxed);
    }

    /**
     * Test assigning a type to a type variable.
     * 
     * @param <T>
     *            The type variable.
     */
    @Test <T> void ilkFromKey() {
        Ilk.Box box = new Ilk.Box(new Ilk<List<T>>() {}.assign((TypeVariable<?>) new Ilk<T>() {}.key.type, String.class).key.type);
        Ilk<List<String>> ilkString = box.cast(new Ilk<Ilk<List<String>>>() {});
        System.out.println(ilkString);
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
        Ilk.Key collectable = new Ilk<IntegerComparable>() { }.key;
        assertEquals(getSuperKey(collectable, Comparable.class).toString(), "java.lang.Comparable<java.lang.Integer>");
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
    
    /** Test get actual type against the generic type of a field. */
    @Test
    public void genericType() throws SecurityException, NoSuchFieldException {
        Ilk<Four> ilk = new Ilk<Four>(){};
        Type type = Types.getActualType(Four.class.getField("strings").getGenericType(), ilk.key.type, new LinkedList<Map<TypeVariable<?>, Type>>());
        assertEquals(type.toString(), "java.util.List<java.lang.String>");
    }
    
//    public <T> Ilk<List<List<T>>> testTypeVariableAssignment(Type type) {
//        for (Method method : getClass().getMethods()) {
//            if (method.getName().equals("testTypeVariableAssignment")) {
//                return new Ilk<List<List<T>>>() {}.assign(method.getTypeParameters(), type);
//            }
//        }
//    }
}

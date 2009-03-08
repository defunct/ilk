package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.testng.annotations.Test;

public class IlkTest
{
    @Test
    public void constructor()
    {
        Ilk.Key key = new Ilk<Map<String, List<Integer>>>() { }.key;
        Map<Ilk.Key, Integer> map = new HashMap<Ilk.Key, Integer>();
        map.put(key, 1);
        assertEquals((int) map.get(key), 1);
    }
    
    @Test
    public void canContain()
    {
        Ilk.Key mapKey = new Ilk<Map<String, List<Integer>>>() { }.key;
        assertTrue(mapKey.get(1).getKey().equals(new Ilk<List<Integer>>() { }.key));
        assertFalse(mapKey.get(1).getKey().equals(new Ilk<List<String>>() { }.key));
    }
    
    @Test
    public void assignment()
    {
        Ilk.Key to = new Ilk<Map<Number, List<String>>>() { }.key;
        Ilk.Key from = new Ilk<TreeMap<Integer, ArrayList<String>>>() { }.key;
        assertTrue(to.isAssignableFrom(from));
        assertFalse(to.isAssignableFrom(new Ilk<TreeMap<Integer, HashSet<String>>>() { }.key));
    }
    
    @Test void pair()
    {
        Ilk.Pair pair = new Ilk<String>() { }.pair("Hello, World!");
        String hello = pair.cast(new Ilk<String>() { });
        assertEquals(hello, "Hello, World!");
        replace(new Ilk<String>() { });
    }
    
    private <T> void replace(Ilk<T> ilk) 
    {
        Ilk<Map<T, Integer>> mapKey = new Ilk<Map<T, Integer>>(ilk.key) { };
        Ilk.Pair pair = mapKey.pair(new HashMap<T, Integer>());
        Map<String, Integer> map = pair.cast(new Ilk<Map<String, Integer>>() { });
        assertTrue(map.isEmpty());
    }
}

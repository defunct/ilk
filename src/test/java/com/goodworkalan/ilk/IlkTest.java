package com.goodworkalan.ilk;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        assertTrue(mapKey.get(1).equals(new Ilk<List<Integer>>() { }.key));
        assertFalse(mapKey.get(1).equals(new Ilk<List<String>>() { }.key));
    }
}

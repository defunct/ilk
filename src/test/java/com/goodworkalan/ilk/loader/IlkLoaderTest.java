package com.goodworkalan.ilk.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.Ilk;

/**
 * Unit tests for the {@link IlkLoader} class.
 *
 * @author Alan Gutierrez
 */
public class IlkLoaderTest {
    /** Test load. */
    @Test
    public void fromString() throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Class<?>> imports = new HashMap<String, Class<?>>();
        imports.put("List", List.class);
        imports.put("Map", Map.class);
        imports.put("String", String.class);
        IlkLoader.fromString(classLoader,"Map<List<String>, Map<String, List<String>>>", imports).cast(new Ilk<Ilk<Map<List<String>, Map<String, List<String>>>>>() {});
        IlkLoader.fromString(classLoader,"java.util.Map<java.lang.String, java.lang.String>", imports).cast(new Ilk<Ilk<Map<String, String>>>() {});
        IlkLoader.fromString(classLoader,"java.util.List<java.lang.String>", imports).cast(new Ilk<Ilk<List<String>>>() {});
        IlkLoader.fromString(classLoader,"java.lang.String", imports).cast(new Ilk<Ilk<String>>() {});
    }
}

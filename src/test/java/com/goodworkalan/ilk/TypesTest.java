package com.goodworkalan.ilk;

import static org.testng.AssertJUnit.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * Unit tests for the {@link UncheckedCast} class.
 * <p>
 * <h3>Testing Hash Codes</h3>
 * <p>
 * It is difficult to test hash codes, since the hash code for
 * <code>Class</code> appears to be undefined. We can't create a fixture, to say
 * that a particular hash code value is expected. We can only unroll the looping
 * with the <code>Types.hashCode</code> method in our test and see that the loop
 * produces what is expected.
 * <p>
 * This is rather frivolous, but we do it anyway, we love our testing so.
 * <p>
 * As always, an method that has a checked exception simply throws
 * <code>Exception</code>. I'm not interested in the clerical details of method
 * signatures that are only ever called through a reflection by a test runner.
 * 
 * @author Alan Gutierrez
 */
public class TypesTest {
    /** An actualized map type. */
    public final Map<Integer, String> mapIntString = null;
    
    /** Construct the {@link Types} class to satisfy coverage. */
    @Test
    public void constructor() {
        new Types();
    }
    
    /** The class hash code is the same as its hash code alone. */
    @Test
    public void classHashCode() {
        assertEquals(String.class.hashCode(), Types.hashCode(String.class));
    }

    /**
     * The hash code for a parameterized type matches the unrolled logic for a
     * parameterized type hash code.
     */
    @Test
    public void parameterizedTypeHashCode() throws Exception {
        Field field = getClass().getField("mapIntString");
        ParameterizedType pt = (ParameterizedType) field.getGenericType();
        Type[] actualTypeArguments = pt.getActualTypeArguments();
        int hashCode = pt.getRawType().hashCode() ^ ((actualTypeArguments[0].hashCode() * 37) ^ actualTypeArguments[1].hashCode());
        assertEquals(hashCode, Types.hashCode(pt));
    }
}

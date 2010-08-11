package com.goodworkalan.ilk;

import static org.testng.AssertJUnit.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
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
    
    /** An actualized generic array type. */
    public final List<String>[] arrayListString = null;
    
    /** A method with a generic return type. */
    public <T> T genericReturnType() {
        return null;
    }
    
    /** Construct the {@link Types} class to satisfy coverage. */
    @Test
    public void constructor() {
        new Types();
    }
    
    /** The hash code of null is zero. */
    @Test
    public void nullHashCode() {
        assertEquals(0, Types.hashCode((Type) null));
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

    /**
     * The hash code for a generic array type is the hash code of the array
     * element type. Since we've shown that the parameterized type hash code is
     * correct, we can show that that hash code for a generic array type is
     * correct by comparing the hash code of the array type against the hash
     * code of its element type.
     */
    @Test
    public void genericArrayTypeHashCode() throws Exception {
        Field field = getClass().getField("arrayListString");
        GenericArrayType gat = (GenericArrayType) field.getGenericType();
        assertEquals(Types.hashCode(gat.getGenericComponentType()), Types.hashCode(gat));
    }
    
    /**
     * The hash code for a type variable is like the hash code for parameterized
     * type, the various participants are combined using bitwise or, so we simply
     * use the unrolled loop as a fixture.
     */
    @Test
    public void classTypeVariableHashCode() {
        TypeVariable<?> type = List.class.getTypeParameters()[0];
        int hashCode = Object.class.hashCode() ^ type.getName().hashCode() ^ List.class.hashCode();
        assertEquals(hashCode, Types.hashCode(type));
    }
    
    /**
     * The hash code for a type variable is like the hash code for parameterized
     * type, the various participants are combined using bitwise or, so we simply
     * use the unrolled loop as a fixture.
     */
    @Test
    public void methodTypeVariableHashCode() throws Exception {
        Method method = getClass().getMethod("genericReturnType");
        TypeVariable<?> type = (TypeVariable<?>) method.getGenericReturnType();
        int hashCode = Object.class.hashCode() ^ type.getName().hashCode() ^ method.hashCode();
        assertEquals(hashCode, Types.hashCode(type));
    }
    
    /**
     * The hash code of a wildcard is the hash code of the upper and lower
     * bounds combined with bitwise or.
     */
    @Test
    public void wildcardHashCode() {
        TypeVariable<?> tv = SuperWild.class.getTypeParameters()[0];
        ParameterizedType pt = (ParameterizedType) tv.getBounds()[0];
        WildcardType wt = (WildcardType) pt.getActualTypeArguments()[0];
        int hashCode = wt.getLowerBounds().hashCode() ^ wt.getUpperBounds().hashCode();
        assertEquals(hashCode, Types.hashCode(wt));
    }
}

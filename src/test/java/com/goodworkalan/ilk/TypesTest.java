package com.goodworkalan.ilk;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

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
    
    /** An actualized list type. */
    public final List<Integer> listInt = null;
    
    /** An actualized list type. */
    public final List<String> listString = null;
    
    /** An actualized generic array type. */
    public final List<String>[] arrayListString = null;
    
    /** An actualized generic array type. */
    public final List<Number>[] arrayListNumber = null;
    
    /** A method with a generic return type. */
    public <T> T genericReturnType() {
        return null;
    }
    
    /** Construct the {@link Types} class to satisfy coverage. */
    @Test
    public void constructor() {
        new Types();
    }
    
    /** The raw class for class is the class itself. */
    @Test
    public void classGetRawClass() {
        assertEquals(String.class, Types.getRawClass(String.class));
    }
    
    /** The raw class for a parameterized type is its raw class member. */
    @Test
    public void parameterizedTypeGetRawClass() throws Exception {
        Field field = getClass().getField("mapIntString");
        assertEquals(Map.class, Types.getRawClass(field.getGenericType()));
    }
    
    /** The raw class for any other type is null. */
    @Test
    public void otherTypeGetRawClass() throws Exception {
        Field field = getClass().getField("arrayListString");
        assertNull(Types.getRawClass(field.getGenericType()));
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
        assertEquals(type.hashCode(), Types.hashCode(type));
    }
    
    /**
     * Since we never create new type variables, we use the hash code and equality
     * defined by the JDK.
     */
    @Test
    public void methodTypeVariableHashCode() throws Exception {
        Method method = getClass().getMethod("genericReturnType");
        TypeVariable<?> type = (TypeVariable<?>) method.getGenericReturnType();
        assertEquals(type.hashCode(), Types.hashCode(type));
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
        int hashCode = Types.hashCode(wt.getLowerBounds()) ^ Types.hashCode(wt.getUpperBounds());
        assertEquals(hashCode, Types.hashCode(wt));
    }

    /**
     * Null is equal to null for our purposes.
     */
    @Test
    public void nullEquality() {
        assertTrue(Types.equals(null, null));
        assertFalse(Types.equals(null, String.class));
        assertFalse(Types.equals(String.class, null));
    }

    /**
     * Class equality is tested using <code>Object.equals</code> of the class
     * itself.
     */
    @Test
    public void classEquality() {
        assertTrue(Types.equals(String.class, String.class));
        assertFalse(Types.equals(Number.class, String.class));
    }
    
    public final List<Three<String>.Four<Integer>> threeStringFourInteger = null;
    public final List<Three<Integer>.Four<Integer>> threeIntegerFourInteger = null;
    
    /**
     * Two parameterized types are equal if the raw type, actual type and all
     * actual type parameters are equal.
     */
    @Test
    public void parameterizedTypeEquality() throws Exception {
        Field field = getClass().getField("mapIntString");
        ParameterizedType pt = (ParameterizedType) field.getGenericType();
        assertTrue(Types.equals(pt, pt));
    }
    
    /** Two parameterized types are not equal if their raw types are not equal. */
    @Test
    public void parameterizedTypeRawTypeNotEqual() throws Exception {
        Field map = getClass().getField("mapIntString");
        Field list = getClass().getField("listInt");
        assertFalse(Types.equals(map.getGenericType(), list.getGenericType()));
    }
    
    /** Two parameterized types are not equal if their owner types are not equal. */
    @Test
    public void parameterizedTypeOwnerTypeNotEquals() throws Exception {
        Field threeString = getClass().getField("threeStringFourInteger");
        Field threeInteger = getClass().getField("threeIntegerFourInteger");
        assertFalse(Types.equals(threeString.getGenericType(), threeInteger.getGenericType()));
    }
    
    /** Two parameterized types are not equal if their actual parameter types are not equal. */
    @Test
    public void parameterizedTypeActualParmaeterTypeNotEquals() throws Exception {
        Field threeString = getClass().getField("listInt");
        Field threeInteger = getClass().getField("listString");
        assertFalse(Types.equals(threeString.getGenericType(), threeInteger.getGenericType()));
    }

    /**
     * Two generic array types are equal if their element types are equal.
     */
    @Test
    public void genericArrayTypeEquality() throws Exception {
        Field field = getClass().getField("arrayListString");
        GenericArrayType gat = (GenericArrayType) field.getGenericType();
        assertTrue(Types.equals(gat, gat));
    }
    
    /**
     * When converted to a string, a class shows only the fully qualified class
     * name.
     */
    @Test
    public void classToString() {
        assertEquals("java.lang.String", Types.typeToString(String.class));
    }
    
    /**
     * When converted to a string, a parameterized type displays its fully
     * qualified type names.
     */
    @Test
    public void parameterizedTypeToString() throws Exception {
        Field field = getClass().getField("mapIntString");
        assertEquals("java.util.Map<java.lang.Integer, java.lang.String>", Types.typeToString(field.getGenericType()));
    }
}

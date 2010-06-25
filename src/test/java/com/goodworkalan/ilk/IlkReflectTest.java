package com.goodworkalan.ilk;

import org.testng.annotations.Test;

/**
 * Unit tests for the {@link IlkReflect} class.
 *
 * @author Alan Gutierrez
 */
public class IlkReflectTest {
    /** Test method parameter assignment. */
    @Test
    public void testMethodParameters() throws Exception {
        IlkReflect.Reflector reflector = new IlkReflect.Reflector();
        IlkReflect.invoke(reflector,IlkReflectTest.class.getMethod("actualize", Ilk.class), new Ilk<IlkReflect>(IlkReflectTest.class).box(null), new Ilk<Integer>(){}.box());
//        box.cast(new Ilk<Ilk<List<List<? extends Integer>>>>() {});
    }
    
    /** Classes never actually have type parameters. */
    @Test
    public void whatSortOfTypeIsWappedWhenClassIsUsed() {
        System.out.println(new Ilk<Class<Object>>(){}.key.type);
    }
    

    /**
     * Here is a little conceptual backwater. How do you create an ilk that
     * boxes a class? Not objects of the class, but a class itself. The
     * constructor for such an Ilk requires a parameterized type, but I can't
     * seem to find way to construct such a thing. I can create an Ilk, but
     * there is no good way to get the parameterized into a box. If I get type
     * directly, it is a type and I can only cast it to a wildcard class. I try
     * to use {@link Class#asSubclass(Class)}, but that returns a class, not a
     * parameterized type. I've tried different flavors of substitution, but the
     * acutalization is also expecting a paramterized type.
     */
    @Test(enabled = false)
    public void workWithClasses() throws Exception {
        // XXX When you use ilk, you're always adding an extra parameter.
        Class<? extends Object> o = Object.class.asSubclass(Object.class);
        Class<? extends Integer> i = Integer.class.asSubclass(Integer.class);
        i.asSubclass(o);
        Ilk.Box boxedObjectClass = makeClass(new Ilk<Class<Object>>() {}, Object.class.asSubclass(Object.class));
        Ilk.Box boxedIntegerClass = makeClass(new Ilk<Class<Integer>>() {}, Integer.class);
        Ilk.Box ilkSubClassBoxed = IlkReflect.invoke(IlkReflect.REFLECTOR, Class.class.getMethod("asSubclass", Class.class), boxedObjectClass, boxedIntegerClass);
        System.out.println(ilkSubClassBoxed);
    }
    
    /** Example type parameterized method. */
    public <V> Ilk.Box makeClass(Ilk<Class<V>> subClass, Class<? extends V> sc) throws Exception {
        Ilk<Class<? extends V>> ilkSubClass = new Ilk<Class<? extends V>>() {}.assign(new Ilk<V>() {}, sc);
        return ilkSubClass.box(sc);
    }
}

package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.Types;

/**
 * Provides a specific instance of a type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The type to vend.
 */
class InstanceVendor<I> extends Vendor<I> {
    /** The instance. */
    private final Ilk.Box instance;
    
    /**
     * Create a provider that always returns the given instance.
     * 
     * @param instance
     *            The instance.
     */
    public InstanceVendor(Ilk<I> ilk, Ilk.Box instance, Class<? extends Annotation> qualifier) {
        super(ilk, qualifier, null, null);
        this.instance = instance;
    }

    /**
     * Get the boxed instance. The injector is ignored.
     * 
     * @param injector
     *            The injector.
     * @return The boxed instance.
     */
    public Ilk.Box get(Injector injector) {
        return instance;
    }
    
    /**
     * Create an implementation vendor from type keys.
     * 
     * @param <K>
     *            The local type variable.
     * @param iface
     *            The interface.
     * @param implmentation
     *            The implementation.
     * @param qualifier
     *            The qualifier.
     * @param scope
     *            The scope.
     * @return An implementation vendor.
     */
    public static <K> Vendor<?> instance(Ilk.Box box, Ilk.Key iface, Class<? extends Annotation> qualifier) {
        TypeVariable<?> tv = Types.getMethods(InstanceVendor.class, "instance")[0].getTypeParameters()[0];
        Ilk<InstanceVendor<K>> vendorIlk = new Ilk<InstanceVendor<K>>() {}.assign(tv, iface.type);
        Ilk.Box boxedQualifier = annotationIlk().box(qualifier);
        Ilk.Box boxedBox = new Ilk<Ilk.Box>(Ilk.Box.class).box(box);
        return Injector.needsIlkConstructor(IlkReflect.REFLECTOR,  vendorIlk.key, iface.type, boxedBox, boxedQualifier).cast(vendorIlk);
    }

    private static Ilk<Class<? extends Annotation>> annotationIlk() {
        return new Ilk<Class<? extends Annotation>>() {};
    }
}

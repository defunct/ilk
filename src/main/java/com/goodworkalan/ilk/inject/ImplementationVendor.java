package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.TypeVariable;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.Types;

/**
 * Vender for a specific implementation of an interface. This vendor will
 * construct a new instance of the requested implementation.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The interface type.
 */
class ImplementationVendor<I> extends Vendor<I> {
    /** The type key for the implementation. */
    private final Ilk.Key implementation;

    /**
     * Create an implementation vendor that binds the given super type token to
     * the given implementation.
     * 
     * @param ilk
     *            The super type token of the binding.
     * @param implementation
     *            The implementation.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope in which to store the constructed object.
     * @param reflector
     *            The reflector to use to construct the instance and invoke the
     *            setters.
     */
    public ImplementationVendor(Ilk<I> ilk, Ilk.Key implementation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, IlkReflect.Reflector reflector) {
        super(ilk, qualifier, scope, reflector);
        this.implementation = implementation;
    }

    /**
     * Get a boxed instance of the implementation using the given injector.
     * 
     * @param injector
     *            The injector.
     * @throws InstantiationException
     *             If the implementation is abstract.
     * @throws IllegalAccessException
     *             If the constructor is inaccessible.
     * @throws InvocationTargetException
     *             If an excpetion is raised by the constructor.
     */
    @Override
    public Ilk.Box get(Injector injector) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return injector.newInstance(reflector, implementation);
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
    public static <K> Vendor<?> implementation(Ilk.Key iface, Ilk.Key implmentation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        TypeVariable<?> tv = Types.getMethods(ImplementationVendor.class, "implementation")[0].getTypeParameters()[0];
        Ilk<ImplementationVendor<K>> vendorIlk = new Ilk<ImplementationVendor<K>>() {}.assign(tv, iface.type);
        Ilk.Key vendorKey = vendorIlk.key;
        Ilk.Box boxedImplementation = new Ilk<Ilk.Key>(Ilk.Key.class).box(implmentation);
        Ilk<Class<? extends Annotation>> annotationIlk = new Ilk<Class<? extends Annotation>>() {};
        Ilk.Box boxedQualifier = annotationIlk.box(qualifier);
        Ilk.Box boxedScope = annotationIlk.box(scope);
        Ilk.Box boxedReflector = new Ilk<IlkReflect.Reflector>(IlkReflect.Reflector.class).box(IlkReflect.REFLECTOR);
        return Injector.needsIlkConstructor(IlkReflect.REFLECTOR, vendorKey, iface.type, boxedImplementation, boxedQualifier, boxedScope, boxedReflector).cast(vendorIlk);
    }
}
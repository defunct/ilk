package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.Types.getRawClass;
import static com.goodworkalan.ilk.inject.InjectException._;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Qualifier;
import javax.inject.Scope;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;

/**
 * Supplies an instance of an object or a <code>Provider&lt;T&gt;</code>.
 * <p>
 * This internal interface is used in lieu of using
 * {@link javax.inject.Provider Provider&lt;T&gt;} directly. Instead of
 * providing object instances directly, the <code>Vendor</code> interface
 * encapsulates objects in type-safe {@link com.goodworkalan.ilk.Ilk.Box}
 * containers. The <code>Ilk.Box</code> will preserve the actual type
 * information for generic types, so that generic objects can be checked for
 * assignability before they are returned by the injector or injected.
 * 
 * @author Alan Gutierrez
 */
public abstract class Vendor<I> {
    /** The super type token of the type to vend. */
    protected final Ilk<I> ilk;
    
    /** The binding qualifier. */
    protected final Class<? extends Annotation> qualifier;
    
    /**  The scope in which to store the constructed object. */
    protected final Class<? extends Annotation> scope;
    
    /**
     * The reflector to use to construct the instance and invoke the setters.
     */
    protected final IlkReflect.Reflector reflector;

    /**
     * Create a vendor with the given super type token.
     * <p>
     * Checks that the given annotation is a scope annotation, or if it is null
     * convert it into a hidden no-scope annotation.
     * <p>
     * Check that the given annotation is a qualifier annotation, or if it is
     * null convert it into a hidden no-qualifier annotation.
     * 
     * @param ilk
     *            The super type token of the binding.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope in which to store the constructed object.
     * @param reflector
     *            The reflector to use to construct the instance and invoke the
     *            setters.
     */
    protected Vendor(Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, IlkReflect.Reflector reflector) {
        if (qualifier == null) {
            qualifier = NoQualifier.class;
        }
        if (!qualifier.equals(NoQualifier.class) && qualifier.getAnnotation(Qualifier.class) == null) {
            throw new IllegalArgumentException();
        }
        if (scope == null) {
            scope = NoScope.class;
        }
        if (scope.equals(NoScope.class)) {
            for (Annotation annotation : getRawClass(ilk.key.type).getAnnotations()) {
                if (annotation.annotationType().getAnnotation(Scope.class) != null) {
                    scope = annotation.annotationType();
                    break;
                }
            }
        } else if (scope.getAnnotation(Scope.class) == null) {
            throw new IllegalArgumentException();
        }
        this.ilk = ilk;
        this.qualifier = qualifier;
        this.scope = scope;
        this.reflector = reflector == null ? IlkReflect.REFLECTOR : reflector;
    }

    /**
     * Create an unscoped instance of the the implementation provided by this
     * vendor.
     * 
     * @param injector
     *            The injector.
     * @return A boxed instance of the implementation.
     * @throws InstantiationException
     *             If the implementation is abstract.
     * @throws IllegalAccessException
     *             If the implementation class or its injected constructor are
     *             inaccessible.
     * @throws InvocationTargetException
     *             If the constructor throws an exception.
     */
    public abstract Ilk.Box get(Injector injector) throws InstantiationException, IllegalAccessException, InvocationTargetException;
    
    /**
     * Supply an instance of an object using the given injector to obtain an
     * injected parameters.
     * 
     * @param injector
     *            The dependency injector.
     * @return An object instance boxed with its actual type information.
     */
    Ilk.Box instance(Injector injector) {
        boolean success = false;
        injector.startInjection();
        try {
            Ilk.Box box = null;
            if (!NoScope.class.equals(scope)) {
                box = injector.getBoxOrLockScope(ilk.key, qualifier, scope);
            }
            if (box == null) {
                try {
                    box = get(injector);
                } catch (Throwable e) {
                  throw new InjectException(_("Unable to create new instance of [%s].", e, getRawClass(ilk.key.type)), e);
                }
                if (getClass().equals(ImplementationVendor.class)) {
                    injector.queueForSetterInjection(box, reflector);
                }
                if (!NoScope.class.equals(scope)) {
                    injector.addBoxToScope(ilk.key, qualifier, scope, box);
                }
            }
            success = true;
            return box;
        } finally {
            injector.endInjection(success);
        }
    }

    /**
     * Construct a provider using reflection in order to preserve the actual
     * type information. The reflective methods of <code>Ilk.Key</code> will
     * check the actual type information in the <code>Ilk.Box</code> parameters
     * against the actual type information in the type contained by the key. It
     * will return the newly constructed objects encapsulated in a
     * <code>Ilk.Box</code> with their actual type information.
     * 
     * @param injector
     *            The injector.
     * @return A boxed instance of the provider.
     * @throws InvocationTargetException
     *             If the provider constructor raises an exception.
     * @throws IllegalAccessException
     *             If the provider class or the constructor an inaccessible.
     * @throws InstantiationException
     *             If the provider class is abstract.
     */
    Ilk.Box provider(Injector injector) {
        Ilk.Key provider = new Ilk<VendorProvider<I>>() { }.assign(new Ilk<Ilk<I>>() {}, ilk).key;
        Type type = ((ParameterizedType) provider.type).getActualTypeArguments()[0];
        Ilk.Box boxedVendor = new Ilk<Vendor<I>>() {}.assign(new Ilk<Ilk<I>>() {}, ilk).box(this);
        Ilk.Box boxedInjector = new Ilk<Injector>(Injector.class).box(injector);
        return Injector.needsIlkConstructor(IlkReflect.REFLECTOR, provider, type, boxedVendor, boxedInjector);
    }
}

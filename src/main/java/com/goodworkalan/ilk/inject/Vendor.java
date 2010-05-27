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
 * encapsulates objects in type-safe {@link IlkReflect.Box} containers. The
 * <code>Ilk.Box</code> will preserve the actual type information for generic
 * types, so that generic objects can be checked for assignability before they
 * are returned by the injector or injected.
 * 
 * @author Alan Gutierrez
 */
public abstract class Vendor<I> {
    /** The super type token of the type to vend. */
    protected final Ilk<I> ilk;
    
    protected final Class<? extends Annotation> qualifier;
    
    protected final Class<? extends Annotation> scope;
    
    protected final IlkReflect.Reflector reflector;

    /**
     * Create a vendor with the given super type token.
     * <p>
     * Checks that the given annotation is a scope annotation, or if it is null
     * convert it into a hidden no-scope annotation.
     * <p>
     * Check that the given annotation is a qualifier annotation, or if it is
     * null convert it into a hidden no-qualifier annotation.
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
            Ilk.Box box = injector.getBoxOrLockScope(ilk.key, qualifier, scope);
            if (box == null) {
                try {
                    box = get(injector);
                } catch (Throwable e) {
                  throw new InjectException(_("Unable to create new instance of [%s].", e, getRawClass(ilk.key.type)), e);
                }
                // TODO Is there a way to cache them if they are correct?
                // (Probably not since it is really a graph, and sticking
                // objects in the cache is bad if the graph is bad, unless you
                // create a queue of good temporary scopes and that queue gets
                // filled. (No, all constructed, then all setter injected, we
                // would have to check that the setters are good.) You really
                // shouldn't throw exceptions during injection, so scratch that,
                // and a singleton can probably initialize twice.
                injector.addBoxToScope(ilk.key, qualifier, scope, box, reflector);
            }
            success = true;
            return box;
        } finally {
            injector.endInjection(success);
        }
    }

    /**
     * Supply a provoder for an object using the given injector to obtain any
     * injected parameters.
     * 
     * @param injector
     *            The dependency injector.
     * @return An provider instance boxed with its actual type information.
     */

    /**
     * Construct a provider using reflection in order to preserve the actual
     * type information. The reflective methods of <code>Ilk.Key</code> will
     * check the actual type information in the <code>Ilk.Box</code> parameters
     * against the actual type information in the type contained by the key. It will
     * return the newly constructed objects encapsulated in a <code>Ilk.Box</code>
     * with their actual type information.
     */
    Ilk.Box provider(Injector injector) {
        Ilk.Key provider = new Ilk<VendorProvider<I>>() { }.assign(new Ilk<Ilk<I>>() {}, ilk).key;
        Type type = ((ParameterizedType) provider.type).getActualTypeArguments()[0];
        Ilk.Box boxedVendor = new Ilk<Vendor<I>>() {}.assign(new Ilk<Ilk<I>>() {}, ilk).box(this);
        Ilk.Box boxedInjector = new Ilk<Injector>(Injector.class).box(injector);
        return Injector.needsIlkConstructor(IlkReflect.REFLECTOR, provider, type, boxedVendor, boxedInjector);
    }
}

package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.Types.getRawClass;
import static com.goodworkalan.ilk.inject.InjectException._;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.Types;

/**
 * Create object graphs with dependency.
 * 
 * @author Alan Gutierrez
 */
public class Injector {
    /** The type of the scope map. */
    final static Ilk<ConcurrentMap<List<Object>, Ilk.Box>> SCOPE_TYPE = new Ilk<ConcurrentMap<List<Object>, Ilk.Box>>(){};

    /**
     * A weak identity reference used to track owner instances created by
     * injection for nested instances. This object defines equality for use in a
     * map using first the identity of the referenced itself, then the identity
     * of the referenced object, so that the reference itself can be used to
     * remove the entry from the map, after the object has been collected.
     * 
     * @author Alan Gutierrez
     */
    private final class WeakIdentityReference extends WeakReference<Object> {
        /** Cache the hash code of the underlying object. */
        private final int hashCode;

        /**
         * Create a weak identity reference for the given object.
         * 
         * @param object
         *            The object to reference.
         * @param queue
         *            The reference queue used to reap entries.
         */
        public WeakIdentityReference(Object object, ReferenceQueue<Object> queue) {
            super(object, queue);
            this.hashCode = System.identityHashCode(object);
        }

        /**
         * Get the identity hash code of the referenced object.
         * 
         * @return The hash code.
         */
        public int hashCode() {
            return hashCode;
        }

        /**
         * This reference is equal to the given object if it is the same
         * instance as this object, or if the referenced objects are the the
         * same objects.
         * 
         * @param The
         *            object to test for equality.
         * @return True if this object is equal to the given object.
         */
        public boolean equals(Object object) {
            // The test against this will short circuit the dereference when we collect.
            return object == this || get() == ((WeakIdentityReference) object).get();
        }
    }
    
    /** The reference queue for owner instances references. */
    private final ReferenceQueue<Object> ownerInstanceReferences = new ReferenceQueue<Object>();
    
    /** The concurrent map of owner instance references. */
    private final ConcurrentMap<WeakIdentityReference, Ilk.Box> ownerInstances = new ConcurrentHashMap<WeakIdentityReference, Ilk.Box>();

    /**
     * Collect the references to owner instances that have gone out of scope.
     */
    private void collectOwnerObjects() {
        Reference<? extends Object> object;
        while ((object = ownerInstanceReferences.poll()) != null) {
            ownerInstances.remove(object);
        }
    }

    /** The injector parent. */
    private final Injector parent;

    /** The map of types to instructions on how to provide them. */
    private final Map<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>> vendors;
    
    /** The write lock on all scope containers in this injector. */  
    private final Lock scopeLock = new ReentrantLock();
  
    /** The map of scope annotations to concurrent maps of qualified types to boxes. */
    private final Map<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> scopes;

    /** The thread based stack of injection invocations. */
    private final static ThreadLocal<Injection> INJECTION = new ThreadLocal<Injection>() {
        public Injection initialValue() {
            return new Injection();
        }
    };

    /**
     * Create an injector with the given parent the given builders and the given
     * scopes.
     * 
     * @param parent
     *            The parent injector.
     * @param builders
     *            The builder mappings.
     * @param scopes
     *            The scopes collection.
     */
    Injector(Injector parent, Map<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>> builders, Map<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> scopes) {
        this.vendors = new HashMap<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>>();
        this.scopes = new HashMap<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>>();
        if (parent == null && !scopes.containsKey(Singleton.class)) {
            scopes.put(Singleton.class, new ConcurrentHashMap<List<Object>, Ilk.Box>());
        }
        for (Map.Entry<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>> entry : builders.entrySet()) {
            this.vendors.put(entry.getKey(), new HashMap<Ilk.Key, Vendor<?>>(entry.getValue()));
        }
        for (Map.Entry<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> entry : scopes.entrySet()) {
            this.scopes.put(entry.getKey(), new ConcurrentHashMap<List<Object>, Ilk.Box>(entry.getValue()));
        }
        Ilk<Injector> injectorIlk = new Ilk<Injector>(Injector.class);
        this.vendors.get(NoQualifier.class).put(InjectorBuilder.ilk(Injector.class).key, new InstanceVendor<Injector>(injectorIlk, injectorIlk.box(this), null));
        this.parent = parent;
    }
    
    public InjectorBuilder newInjector() {
        return new InjectorBuilder(this);
    }
    
    public Ilk.Box scope(Class<? extends Annotation> scope) {
        return SCOPE_TYPE.box(scopes.get(scope));
    }
    
    public <I> I instance(Vendor<I> vendor) {
        return vendor.instance(this).cast(vendor.ilk);
    }
    
    public Ilk.Box getOwnerInstance(Object object) {
        Ilk.Box box = ownerInstances.get(new WeakIdentityReference(object, null));
        if (box == null && parent != null) {
            return parent.getOwnerInstance(object);
        }
        return box;
    }

    // FIXME Interested in showing injector boundaries if an internal injection exception is thrown.
    public <T> T instance(Class<T> type, Class<? extends Annotation> qualifier) {
        return instance(new Ilk<T>(type), qualifier);
    }
    public <T> T instance(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return instance(ilk.key, qualifier).cast(ilk);
    }
    
    public <T> Provider<T> provider(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return provider(ilk.key, qualifier).cast(new Ilk<Provider<T>>() {}.assign(new Ilk<Ilk<T>>(){}, ilk));
    }
    
    public Ilk.Box inject(IlkReflect.Reflector reflector, Ilk.Box box, Method method)
    throws IllegalAccessException, InvocationTargetException {
        return IlkReflect.invoke(reflector, method, box, arguments(box.key, method.getParameterTypes(), method.getParameterAnnotations(), method.getGenericParameterTypes()));
    }
    
    public void inject(IlkReflect.Reflector reflector, Ilk.Box box, Field field) throws IllegalAccessException {
       IlkReflect.set(reflector, field, box, arguments(box.key, new Class<?>[]{ field.getType() }, new Annotation[][] { field.getAnnotations() }, new Type[] { field.getGenericType() })[0]);
    }
    
    public Ilk.Box instance(Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getVendor(key, annotationClass).instance(this);
    }

    Ilk.Box provider(Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getVendor(key, annotationClass).provider(this);
    }
    
    void startInjection() {
        // High traffic method, so let's collect owner instances here, collect
        // them before we make new ones.
        collectOwnerObjects();
        
        INJECTION.get().injectionDepth++;
    }

    /** FIXME Perfect example of no need to test twice, null injector. */
    void endInjection(boolean success) {
        Injection injection = INJECTION.get();
        if (--injection.injectionDepth == 0 && !injection.setting) {
            injection.setting = true;
            try {
                if (success) {
                    while (!injection.unset.isEmpty()) {
                        Ilk.Box box = injection.unset.remove();
                        IlkReflect.Reflector reflector = injection.reflectors.remove();
                        for (Method method : getRawClass(box.key.type).getMethods()) {
                            if (null != method.getAnnotation(Inject.class)) {
                                try {
                                    inject(reflector, box, method);
                                } catch (Throwable e) {
                                    throw new InjectException(_("Unable to inject method [%s] in class [%s].", e, method.getName(), box.key), e);
                                }
                            }
                        }
                        for (Field field : getRawClass(box.key.type).getFields()) {
                            if (null != field.getAnnotation(Inject.class)) {
                                try {
                                    inject(reflector, box, field);
                                } catch (Throwable e) {
                                    throw new InjectException(_("Unable to inject field [%s] in class [%s].", e, field.getName(), box.key), e);
                                }
                            }
                        }
                    }
                    for (Map.Entry<Class<? extends Annotation>, Map<List<Object>, Ilk.Box>> entry : injection.scopes.entrySet()) {
                        Class<? extends Annotation> scope = entry.getKey();
                        Injector injector = this;
                        while (!injector.scopes.containsKey(scope)) {
                            injector = injector.parent;
                        }
                        injector.scopes.get(scope).putAll(entry.getValue());
                    }
                }
            } finally {
                Injector injector = this;
                for (int i = 0; i < injection.lockHeight; i++) {
                    injector.scopeLock.unlock();
                    injector = injector.parent;
                }
                INJECTION.remove();
            }
        }
    }

    private Vendor<?> getStipulatedVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        Map<Ilk.Key, Vendor<?>> stipulationByIlk = vendors.get(qualifier);
        if (stipulationByIlk != null) {
            Vendor<?> vendor = stipulationByIlk.get(key);
            if (vendor != null) {
                return vendor;
            }
        }
        if (parent != null) {
            return parent.getStipulatedVendor(key, qualifier);
        }
        return null;
    }

    private final ConcurrentMap<List<Object>, Vendor<?>> cache = new ConcurrentHashMap<List<Object>, Vendor<?>>();
    
    public Vendor<?> getVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            qualifier = NoQualifier.class;
        }
        List<Object> qualifiedType = Arrays.<Object>asList(key, qualifier);
        Vendor<?> vendor = cache.get(qualifiedType);
        if (vendor == null) {
            vendor = getUncachedVendor(key, qualifier);
            cache.put(qualifiedType, vendor);
        }
        return vendor;
    }

    private <K> Vendor<?> getUncachedVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        Vendor<?> vendor = getStipulatedVendor(key, qualifier);
        if (vendor == null) {
            if (qualifier.equals(NoQualifier.class)) {
                Ilk<ImplementationVendor<K>> vendorIlk = new Ilk<ImplementationVendor<K>>() {}.assign(new Ilk<K>() {}, key.type);
                Ilk.Key vendorKey = vendorIlk.key;
                Ilk.Box boxedKey = new Ilk<Ilk.Key>(Ilk.Key.class).box(key);
                return needsIlkConstructor(IlkReflect.REFLECTOR, vendorKey, key.type, boxedKey).cast(vendorIlk);
            }
            return getVendor(key, NoQualifier.class);
        }
        return vendor;
    }
    
    private Ilk.Box[] arguments(Ilk.Key type, Class<?>[] rawTypes, Annotation[][] annotations, Type[] genericTypes) {
        final Ilk.Box[] arguments = new Ilk.Box[rawTypes.length];
        for (int i = 0; i < genericTypes.length; i++) {
            Class<? extends Annotation> qualifierClass = null;
            for (Annotation annotation : annotations[i]) {
                if (annotation.annotationType().getAnnotation(Qualifier.class) != null) {
                    qualifierClass = annotation.annotationType();
                    break;
                }
            }
            if (qualifierClass == null) {
                qualifierClass = NoQualifier.class;
            }
            if (Provider.class.isAssignableFrom(getRawClass(genericTypes[i]))) {
                arguments[i] = provider(new Ilk.Key(((ParameterizedType) genericTypes[i]).getActualTypeArguments()[0]), qualifierClass);
            } else if (Boxed.class.isAssignableFrom(getRawClass(genericTypes[i]))) {
                Ilk.Box box = instance(new Ilk.Key(((ParameterizedType) genericTypes[i]).getActualTypeArguments()[0]), qualifierClass);
                try {
                    arguments[i] = IlkReflect.newInstance(IlkReflect.REFLECTOR, new Ilk.Key(genericTypes[i]), Boxed.class.getConstructor(Ilk.Box.class), new Ilk<Ilk.Box>(Ilk.Box.class).box(box));
                } catch (Throwable e) {
                    // This is unlikely, it means some of the Boxed class is
                    // missing or corrupt. Just in case it does occur, we make
                    // sure not to wrap it in an InjectException, because the
                    // caller will not know that this is a problem with the Ilk
                    // Inject library itself, not object graph or the objects in it.
                    throw new RuntimeException(_("Unable to create a [%s] to encapsulate an [%s].", e, Boxed.class, box.key), e);
                }
            } else {
                arguments[i] = instance(new Ilk.Key(genericTypes[i]), qualifierClass);
            }
        }
        return arguments;
    }

    Ilk.Box getBoxOrLockScope(Ilk.Key key, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        if (scope.equals(NoScope.class)) {
            return null;
        }
        Injection injection = INJECTION.get();
        List<Object> qualifedType = Arrays.<Object>asList(key, qualifier);
        if (!injection.scopes.containsKey(scope)) {
            injection.scopes.put(scope, new HashMap<List<Object>, Ilk.Box>());
        }
        Ilk.Box box = injection.scopes.get(scope).get(qualifedType);
        if (box == null) {
            int lockHeight = 1;
            Injector injector = this;
            while (injector != null && !injector.scopes.containsKey(scope)) {
                injector = injector.parent;
                lockHeight++;
            }
            if (injector == null) {
                throw new NoSuchElementException();
            }
            box = injector.scopes.get(scope).get(qualifedType);
            if (box == null) {
                Injector unlocked = this;
                for (int i = 0; i < lockHeight; i++) {
                    if (injection.lockHeight <= i) {
                        unlocked.scopeLock.lock();
                        injection.lockHeight++;
                    }
                    unlocked = unlocked.parent;
                }
                // Try again after locking, but hold our locks just the same.
                box = injection.scopes.get(scope).get(qualifedType);
            }
        }
        return box;
    }

    void addBoxToScope(Ilk.Key key, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, Ilk.Box box, IlkReflect.Reflector reflector) {
        Injection injection = INJECTION.get();
        injection.unset.offer(box);
        injection.reflectors.offer(reflector);
        if (!scope.equals(NoScope.class)) {
            if (injection.scopes.get(scope).containsKey(Arrays.<Object>asList(key, qualifier))) {
                throw new IllegalStateException("Not expecting this state, implies an object that takes itself as a constructor argument.");
            }
            injection.scopes.get(scope).put(Arrays.<Object>asList(key, qualifier), box);
        }
    }
    
    Ilk.Box newInstance(IlkReflect.Reflector reflector, Ilk.Key type) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> injectable = null;
        Constructor<?> noArgument = null;
        for (java.lang.reflect.Constructor<?> constructor : getRawClass(type.type).getConstructors()) {
            if (constructor.getAnnotation(Inject.class) != null) {
                if (injectable != null) {
                    throw new InjectException(_("Multiple injectable constructors found for [%s].", null, getRawClass(type.type)), null);
                }
                injectable = constructor;
            } 
            if (constructor.getParameterTypes().length == (getRawClass(type.type).isMemberClass() ? 1 : 0)) {
                noArgument = constructor;
            }
        }
        if (injectable == null) {
            injectable = noArgument;
        }
        if (injectable == null) {
            throw new InjectException(_("No injectable constructor found for [%s].", null, getRawClass(type.type)), null);
        }
        Ilk.Box[] arguments = arguments(type, injectable.getParameterTypes(), injectable.getParameterAnnotations(), injectable.getGenericParameterTypes());
        Ilk.Box instance = IlkReflect.newInstance(reflector, type, injectable, arguments);
        if (Types.getRawClass(type.type).isMemberClass()) {
            ownerInstances.put(new WeakIdentityReference(instance.object, ownerInstanceReferences), arguments[0]);
        }
        return instance;
    }
    
    /**
     * Construct an object using reflection that accepts an {@link Ilk} as the
     * first constructor parameter.
     * 
     * @param reflector
     *            The reflector to use to create public objects, exposed for
     *            unit testing the unlikely occurrence of a reflection
     *            exception.
     * @param key
     *            The type of object to construct.
     * @param ilk
     *            The type to encapsulate with an <code>Ilk</code>.
     * @param arguments
     *            The additional constructor arguments, boxed.
     * @return A new boxed instance of the object.
     */
    static <T> Ilk.Box needsIlkConstructor(IlkReflect.Reflector reflector, Ilk.Key key, Type unwrapped, Ilk.Box...arguments) {
        try {
            Class<?>[] parameters = new Class<?>[arguments.length + 1];
            final Ilk.Box[] withIlkArguments = new Ilk.Box[arguments.length + 1];

            withIlkArguments[0] = new Ilk<T>(){}.assign(new Ilk<T>() {}, unwrapped).box();
            parameters[0] = Ilk.class;
            
            for (int i = 0; i < arguments.length; i++) {
                withIlkArguments[i + 1] = arguments[i];
                parameters[i + 1] = getRawClass(arguments[i].key.type);
            }
            
            Constructor<?> constructor = getRawClass(key.type).getConstructor(parameters);
            
            return IlkReflect.newInstance(new IlkReflect.Reflector() {
                public Object newInstance(Constructor<?> constructor, Object[] arguments)
                throws InstantiationException, IllegalAccessException, InvocationTargetException {
                    return constructor.newInstance(arguments);
                }
            }, key, constructor, withIlkArguments);
        } catch (Throwable e) {
            // This is unlikely, it means some of the Ilk Inject classes are
            // missing. Just in case it does occur, we make sure not to wrap it
            // in an InjectException, because the caller will not know that this
            // is a problem with the Ilk Inject library itself, not object graph
            // or the objects in it.
            throw new RuntimeException(_("Unlikely reflection exception.", e), e);
        }
    }
}

/* vim: set nowrap: */

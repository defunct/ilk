package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.Types.getRawClass;
import static com.goodworkalan.ilk.inject.InjectException.$;
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
import java.util.LinkedList;
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
    final static Ilk<ConcurrentMap<List<Object>, Ilk.Box>> SCOPE_TYPE = new Ilk<ConcurrentMap<List<Object>, Ilk.Box>>() {};

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
         * @param object
         *            The object to test for equality.
         * @return True if this object is equal to the given object.
         */
        public boolean equals(Object object) {
            // The test against this will short circuit the dereference when we
            // collect.
            return object == this
                    || get() == ((WeakIdentityReference) object).get();
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

    /**
     * The map of scope annotations to concurrent maps of qualified types to
     * boxes.
     */
    private final Map<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> scopes;

    /** The thread based stack of injection invocations. */
    private final static ThreadLocal<LinkedList<Injection>> INJECTIONS = new ThreadLocal<LinkedList<Injection>>(); /*{
        public LinkedList<Injection> initialValue() {
            return new LinkedList<Injection>();
        }
    };*/

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
        for (Map.Entry<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>> entry : builders .entrySet()) {
            this.vendors.put(entry.getKey(), new HashMap<Ilk.Key, Vendor<?>>(entry.getValue()));
        }
        for (Map.Entry<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> entry : scopes .entrySet()) {
            this.scopes.put(entry.getKey(), new ConcurrentHashMap<List<Object>, Ilk.Box>(entry.getValue()));
        }
        Ilk<Injector> injectorIlk = new Ilk<Injector>(Injector.class);
        this.vendors.get(NoQualifier.class).put(InjectorBuilder.ilk(Injector.class).key, new InstanceVendor<Injector>(injectorIlk, injectorIlk.box(this), null));
        this.parent = parent;
    }

    /**
     * Get the parent injector.
     * 
     * @return The parent injector.
     */
    public Injector getParent() {
        return parent;
    }

    /**
     * Create a new injector builder to define an injector that is a child of
     * this injector.
     * 
     * @return A new injector builder.
     */
    public InjectorBuilder newInjector() {
        return new InjectorBuilder(this);
    }

    /**
     * Get the serializable container for the given scope in this injector for
     * preservation between injector instances.
     * 
     * @param scope
     *            The scope annotation
     * @return A serializable container of the scope contents.
     */
    public Ilk.Box scope(Class<? extends Annotation> scope) {
        return SCOPE_TYPE.box(scopes.get(scope));
    }

    /**
     * Get an instance of the given interface using the given vendor. This
     * method is used by the multi-binding implementations to construct
     * collections of instances from collections of vendors. The
     * <code>InjectorBuilder</code> binding methods return the
     * <code>Vendor</code> they define for use in mutli-binding collections.
     * 
     * @param <I>
     *            The interface type.
     * @param vendor
     *            The implementation vendor.
     * @return An instnace of the given type.
     */
    public <I> I instance(Vendor<I> vendor) {
        return vendor.instance(this).cast(vendor.ilk);
    }

    /**
     * Get the boxed owner instance of the given nested object or null if the
     * given object is not nested or was not created by this injector. Instance
     * bindings of nested objects will not have an owner instance reference,
     * because they are not created by the injector.
     * <p>
     * You are able to create nested objects through injection, that will in
     * turn use injection to obtain their owner object. There will be no way to
     * provide an owner object, nor will there be an explicit reference to the
     * object in the nested object that can be referenced from outside the
     * nested object. To obtain the parent object, the injector takes note of
     * the parent object when the nested object is constructed and stores in a
     * weak (identity) hash map keyed on the nested object.
     * 
     * @param object
     *            The nested object.
     * @return The owner object or null if the object is not nested or was not
     *         created by this injector.
     */
    public Ilk.Box getOwnerInstance(Object object) {
        Ilk.Box box = ownerInstances.get(new WeakIdentityReference(object, null));
        if (box == null && parent != null) {
            return parent.getOwnerInstance(object);
        }
        return box;
    }

    /**
     * Get an instance of the the given interface bound to an implementation
     * with the given qualifier, or to the implementation with no qualifier if
     * the binding for the given qualifier does not exist. The qualifier can be
     * null to select the unqualified binding.
     * 
     * @param <T>
     *            Type interface type.
     * @param type
     *            The interface class.
     * @param qualifier
     *            The binding qualifier.
     * @return An instance of the bound implementation.
     */
    public <T> T instance(Class<T> type, Class<? extends Annotation> qualifier) {
        return instance(new Ilk<T>(type), qualifier);
    }

    /**
     * Get an instance of the the given interface bound to an implementation
     * with the given qualifier, or to the implementation with no qualifier if
     * the binding for the given qualifier does not exist. The qualifier can be
     * null to select the unqualified binding.
     * 
     * @param <T>
     *            Type interface type.
     * @param ilk
     *            The interface super type token.
     * @param qualifier
     *            The binding qualifier.
     * @return An instance of the bound implementation.
     */
    public <T> T instance(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return instance(ilk.key, qualifier).cast(ilk);
    }

//    /**
//     * Get an instance of the the given instance provider bound to an
//     * implementation with the given qualifier, or to the implementation with no
//     * qualifier if the binding for the given qualifier does not exist. The
//     * qualifier can be null to select the unqualified binding.
//     * 
//     * @param <T>
//     *            Type interface type.
//     * @param ilk
//     *            The interface super type token.
//     * @param qualifier
//     *            The binding qualifier.
//     * @return An instance of the instance provider for the bound
//     *         implementation.
//     */
//    public <T> Provider<T> provider(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
//        return provider(ilk.key, qualifier).cast(new Ilk<Provider<T>>() {}.assign(new Ilk<Ilk<T>>() {}, ilk));
//    }

    /**
     * Invoke the given method on the given boxed instance using the given
     * reflector providing arguments created by this injector as the method
     * parameters. The is apart from setter injection, which will already have
     * run against any methods annotated with {@link Inject}.
     * 
     * @param reflector
     *            The reflector.
     * @param box
     *            The boxed instance.
     * @param method
     *            The method.
     * @return The boxed method return value.
     * @throws IllegalAccessException
     *             If the method cannot be accessed.
     * @throws InvocationTargetException
     *             If the method raises an exception.
     */
    public Ilk.Box inject(IlkReflect.Reflector reflector, Ilk.Box box, Method method) throws IllegalAccessException, InvocationTargetException {
        return IlkReflect.invoke(reflector, method, box, arguments(method.getParameterTypes(), method.getParameterAnnotations(), method.getGenericParameterTypes()));
    }

    /**
     * Set the given field on the given boxed instance using the given reflector
     * providing a value created by this injector as the field value. The is
     * apart from setter injection, which will already have run against any
     * methods annotated with {@link Inject}.
     * 
     * @param reflector
     *            The reflector.
     * @param box
     *            The boxed instance.
     * @param field
     *            The method.
     * @throws IllegalAccessException
     *             If the method cannot be accessed.
     */
    public void inject(IlkReflect.Reflector reflector, Ilk.Box box, Field field)
    throws IllegalAccessException {
        IlkReflect.set(reflector, field, box, arguments(new Class<?>[] { field.getType() }, new Annotation[][] { field.getAnnotations() }, new Type[] { field.getGenericType() })[0]);
    }

    /**
     * Get a boxed instance of the the interface specified by the given type key
     * bound to an implementation with the given qualifier, or to the
     * implementation with no qualifier if the binding for the given qualifier
     * does not exist. The qualifier can be null to select the unqualified
     * binding.
     * 
     * @param key
     *            The interface type key.
     * @param qualifier
     *            The binding qualifier.
     * @return A boxed instance of the bound implementation.
     */
    public Ilk.Box instance(Ilk.Key key, Class<? extends Annotation> qualifier) {
        return getVendor(key, qualifier).instance(this);
    }

    /**
     * Get a boxed instance of the instance provider for the interface specified
     * by the given type key bound to an implementation with the given
     * qualifier, or to the implementation with no qualifier if the binding for
     * the given qualifier does not exist. The qualifier can be null to select
     * the unqualified binding.
     * 
     * @param key
     *            The interface type key.
     * @param qualifier
     *            The binding qualifier.
     * @return A boxed instance of the instance provider for the bound
     *         implementation.
     */
    Ilk.Box provider(Ilk.Key key, Class<? extends Annotation> qualifier) {
        return getVendor(key, qualifier).provider(this);
    }

    /**
     * Start injection by incrementing the injection depth of the thread local
     * injection state object.
     * <p>
     * This method also collects the weakly keyed map of owner objects, because
     * this is a high traffic method, and because its a good time to collect
     * objects, right before you create new ones.
     */
    void startInjection() {
        // High traffic method, so let's collect owner instances here, collect
        // them before we make new ones.
        collectOwnerObjects();

        // Much smaller than deriving from ThreadLocal to define initialzer.
        LinkedList<Injection> injections = INJECTIONS.get();

        if (injections == null) {
            injections = new LinkedList<Injection>();
            injections.addLast(new Injection());
            INJECTIONS.set(injections);
        }

        injections.getLast().injectionDepth++;
    }

    /**
     * Indicate that the next injection is an independent object graph and that
     * the injector should invoke setter injection on all subsequently created
     * instances before returning.
     */
    void setSetterInjectionBoundary() {
        LinkedList<Injection> injections = INJECTIONS.get();
        if (injections == null) {
            injections = new LinkedList<Injection>();
            INJECTIONS.set(injections);
        }
        injections.addLast(new Injection());
    }

    /**
     * Complete an injection by first invoking setters and caching results if
     * the injection was a constructor injection and it was successful, then by
     * unlocking any locked scopes.
     * 
     * @param success
     *            Whether the injection was successful.
     */
    void endInjection(boolean success) {
        LinkedList<Injection> injections = INJECTIONS.get();
        Injection injection = injections.getLast();
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
                                    throw new InjectException($(e), _("Unable to inject method [%s] in class [%s].",  method.getName(), box.key));
                                }
                            }
                        }
                        for (Field field : getRawClass(box.key.type).getFields()) {
                            if (null != field.getAnnotation(Inject.class)) {
                                try {
                                    inject(reflector, box, field);
                                } catch (Throwable e) {
                                    throw new InjectException($(e), _("Unable to inject field [%s] in class [%s].", field.getName(), box.key));
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
                injections.removeLast();
                if (injections.isEmpty()) {
                    Injector injector = this;
                    for (int i = 0, stop = injection.lockHeight; i < stop; i++) {
                        injector.scopeLock.unlock();
                        injector = injector.parent;
                    }
                    INJECTIONS.remove();
                }
            }
        }
    }

    /**
     * Get the vendor stipulated by the binding defined by the given type key
     * and qualifier. Check the current vendor mappings first, and if no vendor
     * is found, ask the parent for its stipulated vendor, and so on.
     * 
     * @param key
     *            The type key of the binding.
     * @param qualifier
     *            The qualifier of the binding.
     * @return The vendor for the type key and binding.
     */
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

    /** A cache of qualified type keys to vendors. */
    private final ConcurrentMap<List<Object>, Vendor<?>> vendorCache = new ConcurrentHashMap<List<Object>, Vendor<?>>();

    /**
     * Get the vendor bound to the given type key and qualifier, or to the type
     * key with no qualifier if the binding for the given qualifier does not
     * exist. The qualifier can be null to select the unqualified binding.
     * 
     * @param key
     *            The type
     * @param qualifier
     *            The binding qualifier.
     * @return The vendor for the binding.
     */
    public Vendor<?> getVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            qualifier = NoQualifier.class;
        }
        List<Object> qualifiedType = Arrays.<Object> asList(key, qualifier);
        Vendor<?> vendor = vendorCache.get(qualifiedType);
        if (vendor == null) {
            vendor = getUncachedVendor(key, qualifier);
            vendorCache.put(qualifiedType, vendor);
        }
        return vendor;
    }

    /**
     * Search the stipulated bindings of this injector and its parent injectors
     * for the vendor bound to the given type key and qualifier, or to the type
     * key with no qualifier if the binding for the given qualifier does not
     * exist. The qualifier can be null to select the unqualified binding.
     * 
     * @param key
     *            The type
     * @param qualifier
     *            The binding qualifier.
     * @return The vendor for the binding.
     */
    private <K> Vendor<?> getUncachedVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        Vendor<?> vendor = getStipulatedVendor(key, qualifier);
        if (vendor == null) {
            if (qualifier.equals(NoQualifier.class)) {
                return implementation(key, key, qualifier, NoScope.class);
            }
            return getVendor(key, NoQualifier.class);
        }
        return vendor;
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
    static <K> Vendor<?> implementation(Ilk.Key iface, Ilk.Key implmentation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        Ilk<ImplementationVendor<K>> vendorIlk = new Ilk<ImplementationVendor<K>>() {}.assign(new Ilk<K>() {}, iface.type);
        Ilk.Key vendorKey = vendorIlk.key;
        Ilk.Box boxedImplementation = new Ilk<Ilk.Key>(Ilk.Key.class).box(implmentation);
        Ilk<Class<? extends Annotation>> annotationIlk = new Ilk<Class<? extends Annotation>>() {};
        Ilk.Box boxedQualifier = annotationIlk.box(qualifier);
        Ilk.Box boxedScope = annotationIlk.box(scope);
        Ilk.Box boxedReflector = new Ilk<IlkReflect.Reflector>(IlkReflect.Reflector.class).box(IlkReflect.REFLECTOR);
        return needsIlkConstructor(IlkReflect.REFLECTOR, vendorKey, iface.type, boxedImplementation, boxedQualifier, boxedScope, boxedReflector).cast(vendorIlk);
    }

    /**
     * Create an array of boxed instances from this injector for use in
     * injecting a constructor, method or field.
     * 
     * @param rawTypes
     *            The raw classes of the parameters.
     * @param annotations
     *            The annotations on the parameters.
     * @param genericTypes
     *            The generic types of the parameters.
     * @return An array of boxed instances.
     */
    private Ilk.Box[] arguments(Class<?>[] rawTypes, Annotation[][] annotations, Type[] genericTypes) {
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
                arguments[i] = provider(new Ilk.Key(((ParameterizedType) genericTypes[i]) .getActualTypeArguments()[0]), qualifierClass);
            } else if (Boxed.class.isAssignableFrom(getRawClass(genericTypes[i]))) {
                Ilk.Box box = instance(new Ilk.Key(((ParameterizedType) genericTypes[i]).getActualTypeArguments()[0]), qualifierClass);
                try {
                    arguments[i] = IlkReflect.newInstance(IlkReflect.REFLECTOR, new Ilk.Key(genericTypes[i]), Boxed.class.getConstructor(Ilk.Box.class), new Ilk<Ilk.Box>(Ilk.Box.class).box(box));
                } catch (Throwable e) {
                    // This is unlikely, it means some of the Boxed class is
                    // missing or corrupt. Just in case it does occur, we make
                    // sure not to wrap it in an InjectException, because the
                    // caller will not know that this is a problem with the Ilk
                    // Inject library itself, not object graph or the objects in
                    // it.
                    throw new RuntimeException(_("Unable to create a [%s] to encapsulate an [%s].", e, Boxed.class, box.key), e);
                }
            } else {
                arguments[i] = instance(new Ilk.Key(genericTypes[i]), qualifierClass);
            }
        }
        return arguments;
    }

    /**
     * Get the box from a scope or else lock the scope into which the box will
     * be stored.
     * 
     * @param key
     *            The interface type key.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope annotation.
     * @return An instance from the scope, or null if the instance is not yet
     *         cached.
     */
    Ilk.Box getBoxOrLockScope(Ilk.Key key, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        LinkedList<Injection> injections = INJECTIONS.get();
        Injection injection = injections.getLast();
        List<Object> qualifedType = Arrays.<Object> asList(key, qualifier);
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
                    if (injections.getFirst().lockHeight <= i) {
                        unlocked.scopeLock.lock();
                        injections.getFirst().lockHeight++;
                    }
                    unlocked = unlocked.parent;
                }
                // Try again after locking, but hold our locks just the same.
                box = injection.scopes.get(scope).get(qualifedType);
            }
        }
        return box;
    }

    /**
     * Queue the newly created object for addition to a scope.
     * 
     * @param key
     *            The type key.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope.
     * @param box
     *            The newly created object.
     */
    void addBoxToScope(Ilk.Key key, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, Ilk.Box box) {
        Injection injection = INJECTIONS.get().getLast();
        if (injection.scopes.get(scope).containsKey( Arrays.<Object> asList(key, qualifier))) {
            throw new IllegalStateException("Not expecting this state, implies an object that takes itself as a constructor argument.");
        }
        injection.scopes.get(scope).put(Arrays.<Object> asList(key, qualifier), box);
    }

    /**
     * Queue the newly created object for setter injection.
     * 
     * @param box
     *            The newly created object.
     * @param reflector
     *            The reflector to use for setter injection.
     */
    void queueForSetterInjection(Ilk.Box box, IlkReflect.Reflector reflector) {
        Injection injection = INJECTIONS.get().getLast();
        injection.unset.offer(box);
        injection.reflectors.offer(reflector);        
    }

    /**
     * Create a new instance of the given type using the given reflector to
     * invoke the constructor. The constructor is provided arguments created by
     * this injector as constructor parameters.
     * 
     * @param reflector
     *            The reflector.
     * @param type
     *            The type key of the type to create.
     * @return A new instance of the type.
     * @throws InstantiationException
     *             If the type is abstract.
     * @throws IllegalAccessException
     *             The the class or constructor is not accessible.
     * @throws InvocationTargetException
     *             If the constructor throws an exception.
     */
    Ilk.Box newInstance(IlkReflect.Reflector reflector, Ilk.Key type)
    throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> injectable = null;
        Constructor<?> noArgument = null;
        for (java.lang.reflect.Constructor<?> constructor : getRawClass(type.type).getConstructors()) {
            if (constructor.getAnnotation(Inject.class) != null) {
                if (injectable != null) {
                    throw new InjectException(null, _("Multiple injectable constructors found for [%s].", getRawClass(type.type)));
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
            throw new InjectException(null, _("No injectable constructor found for [%s].", getRawClass(type.type)));
        }
        Ilk.Box[] arguments = arguments(injectable.getParameterTypes(), injectable.getParameterAnnotations(), injectable .getGenericParameterTypes());
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
     * @param unwrapped
     *            The type to encapsulate with an <code>Ilk</code>.
     * @param arguments
     *            The additional constructor arguments, boxed.
     * @return A new boxed instance of the object.
     */
    static <T> Ilk.Box needsIlkConstructor(IlkReflect.Reflector reflector, Ilk.Key key, Type unwrapped, Ilk.Box... arguments) {
        try {
            Class<?>[] parameters = new Class<?>[arguments.length + 1];
            final Ilk.Box[] withIlkArguments = new Ilk.Box[arguments.length + 1];

            Ilk<T> tv = new Ilk<T>() {};
            withIlkArguments[0] = tv.assign(tv, unwrapped).box();
            parameters[0] = Ilk.class;

            for (int i = 0; i < arguments.length; i++) {
                withIlkArguments[i + 1] = arguments[i];
                parameters[i + 1] = getRawClass(arguments[i].key.type);
            }

            Constructor<?> constructor = getRawClass(key.type).getConstructor(parameters);

            // Would save 1K to move to the parent package.
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
            throw new RuntimeException(_("Unlikely reflection exception.", e),  e);
        }
    }
}

/* vim: set nowrap: */

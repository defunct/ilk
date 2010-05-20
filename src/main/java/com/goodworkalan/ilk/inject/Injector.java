package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectException.MULTIPLE_INJECTABLE_CONSTRUCTORS;

import java.lang.annotation.Annotation;
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
import com.goodworkalan.ilk.association.IlkAssociation;
import com.goodworkalan.reflective.Reflective;

/**
 * Create object graphs with dependency.
 * 
 * @author Alan Gutierrez
 */
public class Injector {
    /** The type of the scope map. */
    final static Ilk<ConcurrentMap<List<Object>, Ilk.Box>> SCOPE_TYPE = new Ilk<ConcurrentMap<List<Object>, Ilk.Box>>(){};

    /** The injector parent. */
    private final Injector parent;

    /** The map of types to instructions on how to provide them. */
    private final Map<Class<? extends Annotation>, IlkAssociation<Vendor<?>>> vendors;
    
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
    Injector(Injector parent, Map<Class<? extends Annotation>, IlkAssociation<Vendor<?>>> builders, Map<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> scopes) {
        this.vendors = new HashMap<Class<? extends Annotation>, IlkAssociation<Vendor<?>>>();
        this.scopes = new HashMap<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>>();
        if (parent == null && !scopes.containsKey(Singleton.class)) {
            scopes.put(Singleton.class, new ConcurrentHashMap<List<Object>, Ilk.Box>());
        }
        for (Map.Entry<Class<? extends Annotation>, IlkAssociation<Vendor<?>>> entry : builders.entrySet()) {
            this.vendors.put(entry.getKey(), new IlkAssociation<Vendor<?>>(entry.getValue()));
        }
        for (Map.Entry<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> entry : scopes.entrySet()) {
            this.scopes.put(entry.getKey(), new ConcurrentHashMap<List<Object>, Ilk.Box>(entry.getValue()));
        }
        this.vendors.get(NoQualifier.class).assignable(InjectorBuilder.ilk(Injector.class).key, new InstanceVendor<Injector>(InjectorBuilder.ilk(Injector.class), this, null));
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

    // FIXME Interested in showing injector boundaries if an internal injection exception is thrown.
    public <T> T instance(Class<T> type, Class<? extends Annotation> qualifier) {
        return instance(new Ilk<T>(type), qualifier);
    }
    public <T> T instance(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return instance(ilk.key, qualifier).cast(ilk);
    }
    
    public <T> Provider<T> provider(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return provider(ilk.key, qualifier).cast(new Ilk<Provider<T>>(ilk.key) {});
    }
    
    public Ilk.Box inject(Ilk.Box box, Method method) {
        try {
            return box.key.invoke(method, box, arguments(box.key, method.getParameterTypes(), method.getParameterAnnotations(), method.getGenericParameterTypes()));
        } catch (Throwable e) {
            throw new InjectException(Reflective.encode(e), e);
        }
    }
    
    public void inject(Ilk.Box box, Field field) {
        try {
            box.key.set(field, box, arguments(box.key, new Class<?>[]{ field.getType() }, new Annotation[][] { field.getAnnotations() }, new Type[] { field.getGenericType() })[0]);
        } catch (Throwable e) {
            throw new InjectException(Reflective.encode(e), e);
        }
    }
    
    Ilk.Box instance(Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getVendor(key, annotationClass).instance(this);
    }

    Ilk.Box provider(Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getVendor(key, annotationClass).provider(this);
    }
    
    void startInjection() {
        INJECTION.get().injectionDepth++;
    }

    /** FIXME Perfect example of no need to test twice, null injector. */
    void endInjection() {
        Injection injection = INJECTION.get();
        if (--injection.injectionDepth == 0 && !injection.setting) {
            injection.setting = true;
            while (!injection.unset.isEmpty()) {
                Ilk.Box box = injection.unset.remove();
                for (Method method : box.key.rawClass.getMethods()) {
                    if (null != method.getAnnotation(Inject.class)) {
                        inject(box, method);
                        break;
                    }
                }
                for (Field field : box.key.rawClass.getFields()) {
                    if (null != field.getAnnotation(Inject.class)) {
                        inject(box, field);
                        break;
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
            Injector injector = this;
            for (int i = 0; i < injection.lockHeight; i++) {
                injector.scopeLock.unlock();
                injector = injector.parent;
            }
            INJECTION.remove();
        }
    }

    private Vendor<?> getStipulatedVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        IlkAssociation<Vendor<?>> stipulationByIlk = vendors.get(qualifier);
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
    
    private Vendor<?> getVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
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

    static final Ilk<ImplementationVendor<?>> IMPLEMENTATION_VENDOR_WILDCARD = new Ilk<ImplementationVendor<?>>() {};
    
    private Vendor<?> getUncachedVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        Vendor<?> vendor = getStipulatedVendor(key, qualifier);
        if (vendor == null) {
            if (qualifier.equals(NoQualifier.class)) {
                Ilk.Key vendorKey = new Ilk.Key((ParameterizedType) IMPLEMENTATION_VENDOR_WILDCARD.key.type, key);
                Ilk.Box boxedKey = new Ilk<Ilk.Key>(Ilk.Key.class).box(key);
                return needsIlkConstructor(vendorKey, key.type, boxedKey).cast(IMPLEMENTATION_VENDOR_WILDCARD);
            }
            return getVendor(key, NoQualifier.class);
        }
        return vendor;
    }

    private Ilk.Box[] arguments(Ilk.Key type, Class<?>[] rawTypes, Annotation[][] annotations, Type[] genericTypes) {
        final Ilk.Box[] arguments = new Ilk.Box[rawTypes.length];
        Ilk.Key[] keys = type.getKeys(genericTypes);
        for (int i = 0; i < keys.length; i++) {
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
            if (Provider.class.isAssignableFrom(keys[i].rawClass)) {
                arguments[i] = provider(new Ilk.Key(keys[i].getKeys(keys[i].rawClass.getTypeParameters())[0]), qualifierClass);
            } else if (Boxed.class.isAssignableFrom(keys[i].rawClass)) {
                Ilk.Key key = keys[i];
                Ilk.Box box = instance(new Ilk.Key(keys[i].getKeys(keys[i].rawClass.getTypeParameters())[0]), qualifierClass);
                try {
                    arguments[i] = key.newInstance(Ilk.REFLECT, Boxed.class.getConstructor(Ilk.Box.class), box);
                } catch (Throwable e) {
                    throw new InjectException(Reflective.encode(e), e);
                }
            } else {
                arguments[i] = instance(keys[i], qualifierClass);
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
            }
            // Try again after locking, but hold our locks just the same.
            box = injection.scopes.get(scope).get(qualifedType);
        }
        return box;
    }

    void addBoxToScope(Ilk.Key key, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, Ilk.Box box) {
        Injection injection = INJECTION.get();
        injection.unset.offer(box);
        if (!scope.equals(NoScope.class)) {
            injection.scopes.get(scope).put(Arrays.<Object>asList(key, qualifier), box);
        }
    }
    
    Ilk.Box newInstance(Ilk.Key type) {
        Constructor<?> injectable = null;
        Constructor<?> noArgument = null;
        for (java.lang.reflect.Constructor<?> constructor : type.rawClass.getConstructors()) {
            for (Annotation annotation : constructor.getAnnotations()) {
                if (annotation instanceof Inject) {
                    if (injectable != null) {
                        throw new InjectException(MULTIPLE_INJECTABLE_CONSTRUCTORS, null).put("type", type);
                    }
                    injectable = constructor;
                } 
            } 
            if (constructor.getTypeParameters().length == 0) {
                noArgument = constructor;
            }
        }
        if (injectable == null) {
            injectable = noArgument;
        }
        if (injectable == null) {
            throw new InjectException(0, null).put("type", type);
        }
        try {
            return type.newInstance(Ilk.REFLECT, injectable, arguments(type, injectable.getParameterTypes(), injectable.getParameterAnnotations(), injectable.getGenericParameterTypes()));
        } catch (Throwable e) {
            throw new InjectException(Reflective.encode(e), e);
        }
    }
    
    /**
     * Construct an object using reflection that accepts an {@link Ilk} as the
     * first constructor parameter.
     * 
     * @param type
     *            The type of object to construct.
     * @param ilk
     *            The type to encapsulate with an <code>Ilk</code>.
     * @param arguments
     *            The additional constructor arguments, boxed.
     * @return A new boxed instance of the object.
     */
    static Ilk.Box needsIlkConstructor(Ilk.Key type, Type ilk, Ilk.Box...arguments) {
        try {
            Ilk.Box boxedIlk;
            if (ilk instanceof ParameterizedType) {
                Ilk<Ilk<?>> ilkIlk = new Ilk<Ilk<?>>(new Ilk.Key((ParameterizedType) ilk)){};
                Ilk.Box pt = new Ilk<ParameterizedType>(ParameterizedType.class).box((ParameterizedType) ilk);
                Constructor<?> newIlk = Ilk.class.getConstructor(ParameterizedType.class);
                boxedIlk = ilkIlk.key.newInstance(new Ilk.Reflect(), newIlk, pt);
            } else {
                Ilk.Box boxedClass = new Ilk.Box((Class<?>) ilk);
                Ilk<Ilk<?>> ilkIlk = new Ilk<Ilk<?>>(new Ilk.Key((Class<?>) ilk)){};
                Constructor<?> newIlk = Ilk.class.getConstructor(Class.class);
                boxedIlk = ilkIlk.key.newInstance(new Ilk.Reflect(), newIlk, boxedClass);
            }
            Class<?>[] parameters = new Class<?>[arguments.length + 1];
            final Ilk.Box[] withIlkArguments = new Ilk.Box[arguments.length + 1];
            withIlkArguments[0] = boxedIlk;
            parameters[0] = boxedIlk.key.rawClass;
            for (int i = 0; i < arguments.length; i++) {
                withIlkArguments[i + 1] = arguments[i];
                parameters[i + 1] = arguments[i].key.rawClass;
            }
            Constructor<?> newObject = type.rawClass.getConstructor(parameters);
            return type.newInstance(new Ilk.Reflect() {
                public Object newInstance(Constructor<?> constructor, Object[] arguments)
                throws InstantiationException, IllegalAccessException, InvocationTargetException {
                    return constructor.newInstance(arguments);
                }
            }, newObject, withIlkArguments);
        } catch (Throwable e) {
            throw new RuntimeException("Unlikely reflection exception: " + Reflective.encode(e), e);
        }
    }
}

/* vim: set nowrap: */

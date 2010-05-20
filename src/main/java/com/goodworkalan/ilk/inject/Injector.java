package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectException.MULTIPLE_INJECTABLE_CONSTRUCTORS;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
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
import javax.inject.Scope;
import javax.inject.Singleton;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.association.IlkAssociation;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.ReflectiveException;

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
    private final Map<Class<? extends Annotation>, IlkAssociation<Vendor<?>>> builders;
    
    /** The write lock on all scope containers in this injector. */  
    private final Lock scopeLock = new ReentrantLock();
  
    /** The map of scope annotations to concurrent maps of qualified types to boxes. */
    private final Map<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> scopes;

    /** The thread based stack of injection invocations. */
    private final ThreadLocal<Injection> INJECTION = new ThreadLocal<Injection>() {
        public Injection initialValue() {
            return new Injection(scopes.keySet());
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
        this.builders = new HashMap<Class<? extends Annotation>, IlkAssociation<Vendor<?>>>();
        this.scopes = new HashMap<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>>();
        if (parent == null && !scopes.containsKey(Singleton.class)) {
            scopes.put(Singleton.class, new ConcurrentHashMap<List<Object>, Ilk.Box>());
        }
        for (Map.Entry<Class<? extends Annotation>, IlkAssociation<Vendor<?>>> entry : builders.entrySet()) {
            this.builders.put(entry.getKey(), new IlkAssociation<Vendor<?>>(entry.getValue()));
        }
        for (Map.Entry<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> entry : scopes.entrySet()) {
            this.scopes.put(entry.getKey(), new ConcurrentHashMap<List<Object>, Ilk.Box>(entry.getValue()));
        }
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
        return provider(ilk.key, qualifier).cast(provider(ilk));
    }
    
    public Ilk.Box inject(final Ilk.Box boxed, final Method method) {
        try {
            return new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect() throws IllegalAccessException, InvocationTargetException {
                    return boxed.key.invoke(method, boxed, arguments(boxed.key, method.getParameterTypes(), method.getParameterAnnotations(), method.getGenericParameterTypes()));
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
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
        if (--injection.injectionDepth == 0) {
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
    
    Ilk.Box instance(Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getVendor(key, annotationClass).instance(this);
    }

    private Vendor<?> getVendor(Ilk.Key key, Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            qualifier = NoQualifier.class;
        }
        IlkAssociation<Vendor<?>> stipulationByIlk = builders.get(qualifier);
        Vendor<?> vendor = stipulationByIlk.get(key);
        if (vendor == null) {
            if (qualifier.equals(NoQualifier.class)) {
                Ilk.Key vendorKey = new Ilk.Key((ParameterizedType) IMPLEMENTATION_VENDOR_WILDCARD.key.type, key);
                Ilk.Box boxedKey = new Ilk<Ilk.Key>(Ilk.Key.class).box(key);
                Ilk.Box boxedScope = CLASS_ANNOTATION.box(checkScope(key.rawClass));
                Ilk.Box boxedQualifier = CLASS_ANNOTATION.box(qualifier); 
                vendor = needsIlkConstructor(vendorKey, key.type, boxedKey, boxedQualifier, boxedScope).cast(IMPLEMENTATION_VENDOR_WILDCARD);
                stipulationByIlk.cache(key, Collections.<Vendor<?>>singletonList(vendor));
            } else {
                return getVendor(key, NoQualifier.class);
            }
        }
        return vendor;
    }
    
    static final Ilk<ImplementationVendor<?>> IMPLEMENTATION_VENDOR_WILDCARD = new Ilk<ImplementationVendor<?>>() {};

    static final Ilk<Class<? extends Annotation>> CLASS_ANNOTATION = new Ilk<Class<? extends Annotation>>() { };
    
    static Class<? extends Annotation> checkScope(Class<?> rawClass) {
        Class<? extends Annotation> scopeAnnotation = NoScope.class;
        for (Annotation annotation : rawClass.getAnnotations()) {
            for (Annotation annotationAnnotation : annotation.annotationType().getAnnotations()) {
                if (annotationAnnotation.annotationType().equals(Scope.class)) {
                    if (!scopeAnnotation.equals(NoScope.class)) {
                        throw new IllegalStateException();
                    }
                    scopeAnnotation = annotation.annotationType();
                }
            }
        }
        return scopeAnnotation;
    }
    
    static <T> Ilk<Provider<T>> provider(Ilk<T> ilk) {
        return new Ilk<Provider<T>>(ilk.key) {};
    }
    
    private Ilk.Box[] arguments(Ilk.Key type, Class<?>[] rawTypes, Annotation[][] annotations, Type[] genericTypes) {
        final Ilk.Box[] arguments = new Ilk.Box[rawTypes.length];
        Ilk.Key[] keys = type.getKeys(genericTypes);
        for (int i = 0; i < keys.length; i++) {
            Class<? extends Annotation> qualifierClass = null;
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof Qualifier) {
                    if (qualifierClass != null) {
                        throw new InjectException(0);
                    }
                    qualifierClass = annotation.getClass();
                }
            }
            if (qualifierClass == null) {
                qualifierClass = NoQualifier.class;
            }
            if (Provider.class.isAssignableFrom(keys[i].rawClass)) {
                arguments[i] = provider(new Ilk.Key(keys[i].getKeys(keys[i].rawClass.getTypeParameters())[0]), qualifierClass);
            } else if (Boxed.class.isAssignableFrom(keys[i].rawClass)) {
                final Ilk.Key key = keys[i];
                final Ilk.Box box = instance(new Ilk.Key(keys[i].getKeys(keys[i].rawClass.getTypeParameters())[0]), qualifierClass);
                try {
                    arguments[i] = new Reflective().reflect(new Reflection<Ilk.Box>() {
                        public Ilk.Box reflect() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
                            return key.newInstance(new Ilk.Reflect(), Boxed.class.getConstructor(Ilk.Box.class), box );
                        }
                    });
                } catch (ReflectiveException e) {
                    throw new InjectException(0, e);
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
        Ilk.Box box = injection.scopes.get(scope).get(qualifedType);
        if (box == null) {
            Injector injector = this;
            while (injector != null && !injector.scopes.containsKey(scope)) {
                injector = injector.parent;
            }
            if (injector == null) {
                throw new NoSuchElementException();
            }
            box = injector.scopes.get(scope).get(qualifedType);
            if (box == null) {
                int lockCount = 1;
                Injector unlocked = this;
                do {
                    if (injection.lockHeight < lockCount) {
                        injector.scopeLock.lock();
                        injection.lockHeight++;
                    }
                    lockCount++;
                    injector = injector.parent;
                } while (injector != unlocked);
            }
            // Try again after locking, but hold our locks just the same.
            box = injection.scopes.get(scope).get(qualifedType);
        }
        return box;
    }

    void addBoxToScope(Ilk.Key key, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, Ilk.Box box) {
        if (!scope.equals(NoScope.class)) {
            INJECTION.get().scopes.get(scope).put(Arrays.<Object>asList(key, qualifier), box);
        }
    }
    
    Ilk.Box newInstance(final Ilk.Key type) {
        Constructor<?> injectable = null;
        Constructor<?> noArgument = null;
        for (java.lang.reflect.Constructor<?> constructor : type.rawClass.getConstructors()) {
            for (Annotation annotation : constructor.getAnnotations()) {
                if (annotation instanceof Inject) {
                    if (injectable != null) {
                        throw new InjectException(MULTIPLE_INJECTABLE_CONSTRUCTORS, type);
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
            throw new InjectException(0, type);
        }
        final Ilk.Box[] arguments = arguments(type, injectable.getParameterTypes(), injectable.getParameterAnnotations(), injectable.getGenericParameterTypes());
        Ilk.Box instance;
        final Constructor<?> constructor = injectable;
        try {
            instance = new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect()
                throws InstantiationException, IllegalAccessException, InvocationTargetException {
                    return type.newInstance(new Ilk.Reflect(), constructor, arguments);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
        return instance;
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
    static Ilk.Box needsIlkConstructor(final Ilk.Key type, final Type ilk, final Ilk.Box...arguments) {
        try {
            return new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect()
                throws InstantiationException,
                       IllegalAccessException,
                       InvocationTargetException,
                       NoSuchMethodException {
                    Ilk.Box boxedIlk;
                    if (ilk instanceof ParameterizedType) {
                        Ilk<Ilk<?>> ilkIlk = new Ilk<Ilk<?>>(new Ilk.Key((ParameterizedType) ilk)){};
                        Ilk.Box type = new Ilk<ParameterizedType>(ParameterizedType.class).box((ParameterizedType) ilk);
                        Constructor<?> newIlk = Ilk.class.getConstructor(ParameterizedType.class);
                        boxedIlk = ilkIlk.key.newInstance(new Ilk.Reflect(), newIlk, type);
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
                }
            });
        } catch (ReflectiveException e) {
            // This is going to be a programmer error internal to Ilk Inject, so
            // if you see this, please file a bug report.
            throw new RuntimeException(e);
        }
    }
}

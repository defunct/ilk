package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectException.MULTIPLE_INJECTABLE_CONSTRUCTORS;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
    final static Ilk<ConcurrentMap<QualifiedType, Ilk.Box>> SCOPE_TYPE = new Ilk<ConcurrentMap<QualifiedType, Ilk.Box>>(){};

    /** The injector parent. */
    private final Injector parent;

    /** The map of types to instructions on how to provide them. */
    private final Map<Class<? extends Annotation>, IlkAssociation<Vendor>> builders;
    
    /** The write lock on all scope containers in this injector. */  
    private final Lock scopeLock = new ReentrantLock();

    /**
     * The temporary scope where instances are kept while they are being
     * constructed so that partially injected instances are not visible to other
     * threads.
     */
    private final Map<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>> temporaryScopes = new HashMap<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>>();
  
    /** The map of scope annotations to concurrent maps of qualified types to boxes. */
    private final Map<Class<? extends Annotation>, ConcurrentMap<QualifiedType, Ilk.Box>> scopes;

    /** The thread based stack of injection invocations. */
    private final ThreadLocal<LinkedList<Injection>> INJECTIONS = new ThreadLocal<LinkedList<Injection>>() {
        public LinkedList<Injection> initialValue() {
            return new LinkedList<Injection>();
        }
    };

    /**
     * The number of injectors including ancestors and self whose locks are held
     * by this injector instance.
     */
    private int lockHeight;

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
    Injector(Injector parent, Map<Class<? extends Annotation>, IlkAssociation<Vendor>> builders, Map<Class<? extends Annotation>, ConcurrentMap<QualifiedType, Ilk.Box>> scopes) {
        this.builders = new HashMap<Class<? extends Annotation>, IlkAssociation<Vendor>>();
        this.scopes = new HashMap<Class<? extends Annotation>, ConcurrentMap<QualifiedType, Ilk.Box>>();
        if (parent == null && !scopes.containsKey(Singleton.class)) {
            scopes.put(Singleton.class, new ConcurrentHashMap<QualifiedType, Ilk.Box>());
        }
        for (Map.Entry<Class<? extends Annotation>, IlkAssociation<Vendor>> entry : builders.entrySet()) {
            this.builders.put(entry.getKey(), new IlkAssociation<Vendor>(entry.getValue()));
        }
        for (Map.Entry<Class<? extends Annotation>, ConcurrentMap<QualifiedType, Ilk.Box>> entry : scopes.entrySet()) {
            this.scopes.put(entry.getKey(), new ConcurrentHashMap<QualifiedType, Ilk.Box>(entry.getValue()));
            this.temporaryScopes.put(entry.getKey(), new HashMap<QualifiedType, Ilk.Box>());
        }
        this.parent = parent;
    }
    
    Injector(Injector injector) {
        this.builders = injector.builders;
        this.scopes = injector.scopes;
        this.parent = injector.parent;
    }
    
    public InjectorBuilder newInjector() {
        return new InjectorBuilder(this);
    }
    
    public Ilk.Box scope(Class<? extends Annotation> scope) {
        return SCOPE_TYPE.box(scopes.get(scope));
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
        return getBuilder(key, annotationClass).provider(new Injector(this));
    }
    
    Ilk.Box instance(Ilk.Key key, Class<? extends Annotation> annotationClass) {
        Injector injector = new Injector(this);
        Ilk.Box box = getBuilder(key, annotationClass).instance(injector);
        injector.unlockScopes();
        return box;
    }

    private Vendor getBuilder(Ilk.Key key, Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            qualifier = NoQualifier.class;
        }
        IlkAssociation<Vendor> stipulationByIlk = builders.get(qualifier);
        Vendor builder = stipulationByIlk.get(key);
        if (builder == null) {
            if (qualifier.equals(NoQualifier.class)) {
                Ilk.Key providerKey = new Ilk<VendorProvider<?>>(key) { }.key;
                builder = new ImplementationVendor(providerKey, key, key, qualifier, NoScope.class);
                stipulationByIlk.cache(key, Collections.singletonList(builder));
            } else {
                return getBuilder(key, NoQualifier.class);
            }
        }
        return builder;
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
                            return key.newInstance(Boxed.class.getConstructor(Ilk.Box.class), box );
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

    private Ilk.Box getBoxOrLockScope(Class<? extends Annotation> scope, QualifiedType qt) {
        Ilk.Box box = temporaryScopes.get(scope).get(qt);
        if (box == null) {
            Injector injector = this;
            while (injector != null && !injector.scopes.containsKey(scope)) {
                injector = injector.parent;
            }
            if (injector == null) {
                throw new NoSuchElementException();
            }
            box = injector.scopes.get(scope).get(qt);
            if (box == null) {
                int lockCount = 1;
                injector = this;
                while (injector != null && !injector.scopes.containsKey(scope)) {
                    if (lockHeight < lockCount) {
                        injector.scopeLock.lock();
                        lockHeight++;
                    }
                    lockCount++;
                    injector = injector.parent;
                }
            }
        }
        return box;
    }

    /** FIXME Perfect example of no need to test twice, null injector. */
    void unlockScopes() {
        for (Map.Entry<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>> entry : temporaryScopes.entrySet()) {
            Class<? extends Annotation> scope = entry.getKey();
            Injector injector = this;
            while (!injector.scopes.containsKey(scope)) {
                injector = injector.parent;
            }
            injector.scopes.get(scope).putAll(entry.getValue());
            entry.getValue().clear();
        }
        Injector injector = this;
        for (int i = 0; i < lockHeight; i++) {
            injector.scopeLock.unlock();
            injector = injector.parent;
        }
        lockHeight = 0;
    }
    
    Ilk.Box newInstance(Ilk.Key ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        if (scope.equals(NoScope.class)) {
            return newInstance(ilk);
        }
        QualifiedType qt = new QualifiedType(qualifier, ilk);
        Ilk.Box box = getBoxOrLockScope(scope, qt);
        if (box == null) {
            box = newInstance(ilk);
            temporaryScopes.get(scope).put(qt, box);
        }
        return box;
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
                    return type.newInstance(constructor, arguments);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
        return instance;
   }
}

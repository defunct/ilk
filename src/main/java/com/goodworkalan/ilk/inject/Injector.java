package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Qualifier;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;
import com.goodworkalan.ilk.association.IlkAssociation;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.ReflectiveException;

public class Injector {
    final static Ilk<Map<QualifiedType, Ilk.Box>> SCOPE_TYPE = new Ilk<Map<QualifiedType, Ilk.Box>>(){};

    /** The map of types to instructions on how to provide them. */
    private final Map<Class<? extends Annotation>, IlkAssociation<Stipulation>> stipulations = new HashMap<Class<? extends Annotation>, IlkAssociation<Stipulation>>();
    
    final Map<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>> scopes = new HashMap<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>>();
  
    final Injector parent;
    
    Injector(Injector parent, Map<Class<? extends Annotation>, IlkAssociation<Stipulation>> stipulations) {
        for (Map.Entry<Class<? extends Annotation>, IlkAssociation<Stipulation>> entry : stipulations.entrySet()) {
            this.stipulations.put(entry.getKey(), new IlkAssociation<Stipulation>(entry.getValue()));
        }
        this.parent = parent;
    }
    
    public InjectorBuilder newInjector() {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.consume(stipulations);
        return newInjector;
    }
    
    public Ilk.Box scope(Class<? extends Annotation> scope) {
        return SCOPE_TYPE.box(scopes.get(scope));
    }
    
    // FIXME Interested in showing injector boundaries if an internal injection exception is thrown.
    public <T> T create(Class<T> type, Class<? extends Annotation> qualifier) {
        return create(new Ilk<T>(type), qualifier);
    }
    public <T> T create(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return instance(new LinkedList<QualifiedType>(), ilk.key, qualifier).cast(ilk);
    }
    
    public <T> Provider<T> provider(Ilk<T> ilk, Class<? extends Annotation> qualifier) {
        return provider(new LinkedList<QualifiedType>(), ilk.key, qualifier).cast(provider(ilk));
    }
    
    public Ilk.Box inject(final Ilk.Box boxed, final Method method) {
        try {
            return new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect() throws IllegalAccessException, InvocationTargetException {
                    return boxed.key.invoke(method, boxed, arguments(new LinkedList<QualifiedType>(), boxed.key, method.getParameterTypes(), method.getParameterAnnotations(), method.getTypeParameters()));
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
    }
    
    Ilk.Box provider(LinkedList<QualifiedType> stack, Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getStipulation(key, annotationClass).builder.provider(stack, this);
    }
    
    Ilk.Box instance(LinkedList<QualifiedType> stack, Ilk.Key key, Class<? extends Annotation> annotationClass) {
        return getStipulation(key, annotationClass).builder.instance(stack, this);
    }

    private Stipulation getStipulation(Ilk.Key key,
            Class<? extends Annotation> annotationClass) {
        if (annotationClass == null) {
            annotationClass = NoQualifier.class;
        }
        IlkAssociation<Stipulation> stipulationByIlk = stipulations.get(annotationClass);
        Stipulation stipulation = stipulationByIlk.get(key);
        if (stipulation == null) {
            if (!NoQualifier.class.equals(annotationClass)) {
                throw new InjectException(0);
            }
            Ilk.Key providerKey = new Ilk<BuilderProvider<?>>(key) { }.key;
            Builder builder = new NewImplementationBuilder(providerKey, key, key);
            stipulation = new Stipulation(builder, NoScope.class);
            stipulationByIlk.cache(key, Collections.singletonList(stipulation));
        }
        return stipulation;
    }
    
    static <T> Ilk<Provider<T>> provider(Ilk<T> ilk) {
        return new Ilk<Provider<T>>(ilk.key) {};
    }
    
    public Ilk.Box[] arguments(LinkedList<QualifiedType> stack, Ilk.Key type, Class<?>[] rawTypes, Annotation[][] annotations, Type[] genericTypes) {
        final Ilk.Box[] arguments = new Ilk.Box[rawTypes.length];
        Ilk.Key[] keys = type.getActualKeys(genericTypes);
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
                arguments[i] = provider(stack, new Ilk.Key(keys[i].getActualKeys(keys[i].rawClass.getTypeParameters())[0]), qualifierClass);
            } else if (Boxed.class.isAssignableFrom(keys[i].rawClass)) {
                final Ilk.Key key = keys[i];
                final Ilk.Box box = instance(stack, new Ilk.Key(keys[i].getActualKeys(keys[i].rawClass.getTypeParameters())[0]), qualifierClass);
                try {
                    arguments[i] = new Reflective().reflect(new Reflection<Ilk.Box>() {
                        public Box reflect() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
                            return key.newInstance(Boxed.class.getConstructor(Ilk.Box.class), box );
                        }
                    });
                } catch (ReflectiveException e) {
                    throw new InjectException(0, e);
                }
            } else {
                arguments[i] = instance(stack, keys[i], qualifierClass);
            }
        }
        return arguments;
    }
}

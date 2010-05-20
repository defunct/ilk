package com.goodworkalan.ilk.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.ReflectiveException;

public abstract class MissingProviderBuilder implements Builder {
    private final Ilk.Key provider;

    protected final Ilk.Key implementation;

    public MissingProviderBuilder(Ilk.Key provider, Ilk.Key implementation) {
        this.provider = provider;
        this.implementation = implementation;
    }
    
    public Ilk.Box provider(Injector injector) {
        final Ilk.Box boxedBuilder = new Ilk<Builder>(Builder.class).box(this);
        final Ilk.Box boxedInjector = new Ilk<Injector>(Injector.class).box(injector);
        try {
            return new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect()
                throws InstantiationException,
                       IllegalAccessException,
                       InvocationTargetException,
                       NoSuchMethodException {
                    Ilk<Ilk<?>> ilkIlk = new Ilk<Ilk<?>>(implementation){};
                    Ilk.Box boxedIlk;
                    if (implementation.type instanceof ParameterizedType) {
                        Ilk.Box type = new Ilk<ParameterizedType>(ParameterizedType.class).box((ParameterizedType) implementation.type);
                        Constructor<?> newIlk = Ilk.class.getConstructor(ParameterizedType.class);
                        boxedIlk = ilkIlk.key.newInstance(newIlk, type);
                    } else {
                        Ilk.Box boxedClass = new Ilk.Box((Class<?>) implementation.type);
                        Constructor<?> newIlk = Ilk.class.getConstructor(Class.class);
                        boxedIlk = ilkIlk.key.newInstance(newIlk, boxedClass);
                    }
                    Constructor<?> newBuilderProvider = provider.rawClass.getConstructor(Ilk.class, Builder.class, Injector.class);
                    return provider.newInstance(newBuilderProvider, boxedIlk, boxedBuilder, boxedInjector);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
    }
}

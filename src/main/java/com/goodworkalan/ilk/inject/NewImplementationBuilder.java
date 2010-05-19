package com.goodworkalan.ilk.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.LinkedList;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.ReflectiveException;

public class NewImplementationBuilder extends NewInstanceBuilder implements Builder {
    private final Ilk.Key providerKey;

    public NewImplementationBuilder(Ilk.Key providerKey, Ilk.Key iface, Ilk.Key implementation) {
        super(iface, implementation);
        this.providerKey = providerKey;
    }

    public Ilk.Box instance(LinkedList<QualifiedType> stack, Injector injector) {
        return build(stack, injector);
    }
    
    public Ilk.Box provider(LinkedList<QualifiedType> stack, Injector injector) {
        final Ilk.Box boxedStack = new Ilk<LinkedList<QualifiedType>>() {}.box(stack);
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
                    Ilk.Box type = new Ilk<Type>(Type.class).box(implementation.type);
                    Constructor<?> newIlk = Ilk.class.getConstructor(Type.class);
                    Ilk.Box boxedIlk = ilkIlk.key.newInstance(newIlk, type);
                    Constructor<?> newBuilderProvider = providerKey.rawClass.getConstructor(Ilk.class, LinkedList.class, Builder.class, Injector.class);
                    return providerKey.newInstance(newBuilderProvider, boxedIlk, boxedStack, boxedBuilder, boxedInjector);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
    }
}
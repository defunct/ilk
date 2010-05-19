package com.goodworkalan.ilk.inject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.ReflectiveException;

public class NewProviderBuilder extends NewInstanceBuilder implements Builder {
    public NewProviderBuilder(Ilk.Key iface, Ilk.Key implementation) {
        super(iface, implementation);
    }

    public Ilk.Box instance(LinkedList<QualifiedType> stack, Injector injector) {
        final Ilk.Box instance = build(stack, injector);
        try {
            return new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect()
                throws InstantiationException,
                       IllegalAccessException,
                       InvocationTargetException,
                       NoSuchMethodException {
                    Method method = implementation.rawClass.getMethod("get");
                    return instance.key.invoke(method, instance);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
    }
    
    public Ilk.Box provider(LinkedList<QualifiedType> stack, Injector injector) {
        return build(stack, injector);
    }
}

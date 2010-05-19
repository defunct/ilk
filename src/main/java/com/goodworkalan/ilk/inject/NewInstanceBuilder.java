package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectException.MULTIPLE_INJECTABLE_CONSTRUCTORS;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

import javax.inject.Inject;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.ReflectiveException;

public class NewInstanceBuilder {
    protected final Ilk.Key iface;

    protected final Ilk.Key implementation;
    
    public NewInstanceBuilder(Ilk.Key iface, Ilk.Key implementation) {
        this.iface = iface;
        this.implementation = implementation;
    }

    public Ilk.Box build(LinkedList<QualifiedType> stack, Injector injector) {
        Constructor<?> injectable = null;
        Constructor<?> noArgument = null;
        for (java.lang.reflect.Constructor<?> constructor : implementation.rawClass.getConstructors()) {
            for (Annotation annotation : constructor.getAnnotations()) {
                if (annotation instanceof Inject) {
                    if (injectable != null) {
                        throw new InjectException(MULTIPLE_INJECTABLE_CONSTRUCTORS, iface, implementation);
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
            throw new InjectException(0, iface, implementation);
        }
        final Ilk.Box[] arguments = injector.arguments(stack, implementation, injectable.getParameterTypes(), injectable.getParameterAnnotations(), injectable.getTypeParameters());
        Ilk.Box instance;
        final Constructor<?> constructor = injectable;
        try {
            instance = new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect()
                throws InstantiationException, IllegalAccessException, InvocationTargetException {
                    return implementation.newInstance(constructor, arguments);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
        return instance;
   }
}

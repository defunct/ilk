package com.goodworkalan.ilk;

import static com.goodworkalan.ilk.Types.getRawClass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import static java.lang.String.format;
import java.util.Map;

public class IlkReflect {
    private static void getTypeParameters(Member member, Map<TypeVariable<?>, Type> methodTypes, Type argumentType, Type[] types) {
        Type[] actual = new Type[types.length];
        for (int i = 0; i < actual.length; i++) {
            getTypeParameters(member, methodTypes, argumentType, types[i]);
        }
    }

    public static void getTypeParameters(Member member, Map<TypeVariable<?>, Type> types, Type parameterType, Type argumentType) {
        if (parameterType instanceof ParameterizedType) {
            Type[] parameterTypes = ((ParameterizedType) parameterType).getActualTypeArguments();
            Type[] argumentTypes = ((ParameterizedType) argumentType).getActualTypeArguments();
            for (int i = 0; i < parameterTypes.length; i++) {
                getTypeParameters(member, types, parameterTypes[i], argumentTypes[i]);
            }
        } else if (parameterType instanceof TypeVariable<?>) {
            TypeVariable<?> tv = (TypeVariable<?>) parameterType;
            if (tv.getGenericDeclaration().equals(member)) {
                Types.checkTypeVariable(tv, argumentType);
                Type previous = types.get(tv);
                if (previous == null) {
                    types.put(tv, argumentType);
                } else if (!Types.getEquatable(argumentType).equals(previous)) {
                    throw new IllegalArgumentException();
                }
            }
        } else if (parameterType instanceof WildcardType) {
            WildcardType wt = (WildcardType) parameterType;
            getTypeParameters(member, types, argumentType, wt.getLowerBounds());
        }
    }

    public static void checkAssignable(Type to, Type from) {
        if (!getRawClass(to).isAssignableFrom(getRawClass(from))) {
            throw new IllegalArgumentException(format("Unable to assign [%s] from [%s].", getRawClass(to).getName(), getRawClass(from).getName()));
        }
    }
        
    public Ilk.Box newInstance(Reflector reflector, Ilk.Key key)
    throws InstantiationException, IllegalAccessException {
        return new Ilk.Box(key, reflector.newInstance(getRawClass(key.type)));
    }

    /**
     * Create a boxed new instance of type represented by this key using the
     * given constructor with the object values in the given list of boxes as
     * constructor arguments. Each constructor parameters is checked for
     * assignability by checking that the actual constructor parameter is
     * assignable from the key of the boxed argument. If any argument is not
     * assignable to its parameter, an illegal argument exception is raised.
     * 
     * @param constructor
     *            The constructor.
     * @param arguments
     *            The boxed constructor arguments.
     * @return A boxed new instance of the class associated with this key.
     * @throws InstantiationException
     *             If the constructed class is abstract.
     * @throws IllegalAccessException
     *             If the constructor is not accessible.
     * @throws InvocationTargetException
     *             If the constructor raises an exception.
     * @exception IllegalArgumentException
     *                If the number of parameters differ, of if the argument
     *                cannot be assigned to its parameter
     */
    public static Ilk.Box newInstance(Reflector reflector, Ilk.Key key, Constructor<?> constructor, Ilk.Box...arguments)
    throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (!getRawClass(key.type).equals(constructor.getDeclaringClass())) {
            throw new IllegalArgumentException(format("Cannot invoke [%s] on [%s].", getRawClass(key.type).getName(), constructor.getName()));
        }
        Type[] parameters = constructor.getTypeParameters();
        Map<TypeVariable<?>, Type> assignments = new HashMap<TypeVariable<?>, Type>();
        for (int i = 0; i < parameters.length; i++) {
            getTypeParameters(constructor, assignments, parameters[i], arguments[i].key.type);
            Type type = Types.getActualType(parameters[i], key.type);
            type = Types.getActualType(type, assignments);
            if (!Types.isAssignableFrom(type, arguments[i].key.type)) {
                throw new IllegalArgumentException(format("Cannot assign [%s] from [%s].", parameters[i], arguments[i].key.type));
            }
        }
        return new Ilk.Box(key, reflector.newInstance(constructor, objects(arguments)));
    }
    
    private static Object[] objects(Ilk.Box[] boxes) {
        Object[] objects = new Object[boxes.length];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = boxes[i].object;
        }
        return objects;
    }
        
    public static Ilk.Box invoke(Reflector reflector, Method method, Ilk.Box object, Ilk.Box...arguments)
    throws IllegalAccessException, InvocationTargetException {
        checkAssignable(getRawClass(object.key.type), method.getDeclaringClass());
        Type[] parameters = method.getTypeParameters();
        Map<TypeVariable<?>, Type> assignments = new HashMap<TypeVariable<?>, Type>();
        for (int i = 0; i < parameters.length; i++) {
            getTypeParameters(method, assignments, parameters[i], arguments[i].key.type);
            Type actual = Types.getActualType(parameters[i], object.key.type);
            actual = Types.getActualType(actual, assignments);
            if (!Types.isAssignableFrom(actual, arguments[i].key.type)) {
                throw new IllegalArgumentException(format("Cannot assign [%s] from [%s].", parameters[i], arguments[i].key.type));
            }
        }
        Object result = reflector.invoke(method, object.object, objects(arguments));
        Type actual = Types.getActualType(method.getGenericReturnType(), object.key.type);
        actual = Types.getActualType(actual, assignments);
        return enbox(new Ilk.Key(actual), result);
    }
    
    public static void set(Reflector reflector, Field field, Ilk.Box object, Ilk.Box value)
    throws IllegalAccessException {
        checkAssignable(field.getDeclaringClass(), getRawClass(object.key.type));
        Type actual = Types.getActualType(field.getGenericType(), object.key.type);
        if (!Types.isAssignableFrom(actual, value.key.type)) {
            throw new IllegalArgumentException(String.format("Cannot assign [%s] from [%s] in [%s].", field.getGenericType(), object.key, field.getDeclaringClass()));
        }
        reflector.set(field, object.object, value.object);
    }

    /**
     * Create a box for the given key and object or return null if the object is
     * null.
     * 
     * @param key
     *            The key.
     * @param object
     *            The object.
     * @return A box for the key and object or null if the object is null.
     */
    private static Ilk.Box enbox(Ilk.Key key, Object object) {
        if (object == null) {
            return null;
        }
        return new Ilk.Box(key, object);
    }

    public static Ilk.Box get(Reflector reflector, Field field, Ilk.Box object)
    throws IllegalAccessException {
        checkAssignable(field.getDeclaringClass(), getRawClass(object.key.type));
        return enbox(new Ilk.Key(field.getGenericType()), reflector.get(field, object.object));
    }

    public static final Reflector REFLECTOR = new Reflector();
    
    // FIXME This can move out.
    public static class Reflector {
        public Object newInstance(Class<?> type)
        throws InstantiationException, IllegalAccessException {
            return type.newInstance();
        }

        public Object newInstance(Constructor<?> constructor, Object[] arguments)
        throws InstantiationException, IllegalAccessException, InvocationTargetException {
            return constructor.newInstance(arguments);
        }
        
        public Object invoke(Method method, Object object, Object[] arguments)
        throws IllegalAccessException, InvocationTargetException {
            return method.invoke(object, arguments);
        }
        
        public Object get(Field field, Object object)
        throws IllegalAccessException {
            return field.get(object);
        }
        
        public void set(Field field, Object object, Object value)
        throws IllegalAccessException {
            field.set(object, value);
        }
    }
}

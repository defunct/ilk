package com.goodworkalan.ilk;

import static com.goodworkalan.ilk.Types.getRawClass;
import static java.lang.String.format;

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
import java.util.LinkedList;
import java.util.Map;

/**
 * Methods for reflective invocation against boxed object instances that
 * perpetuates type-safety.
 * <p>
 * This is an extension of the <code>com.goodworkalan.ilk</code> package in
 * order to keep the construction of boxed instances package private and type
 * safe. New boxes can be created using type parameters, or through reflection,
 * where reflection is used when the type parameters are no longer visible.
 * 
 * @author Alan Gutierrez
 */
public class IlkReflect {
    /**
     * Iterate over the given array of types from a wildcard definition and map
     * type parameters defined by the given member to the actual type values of
     * the given argument.
     * 
     * @param member
     *            The method or constructor that defined the parameters.
     * @param types
     *            Type map of member type definitions to provided argument
     *            types.
     * @param argumentType
     *            Type type argument given by the caller.
     * @param parameterTypes
     *            Type type parameters defined by the member.
     */
    private static void getTypeParameters(Member member, Map<TypeVariable<?>, Type> types, Type argumentType, Type[] parameterTypes) {
        Type[] actual = new Type[parameterTypes.length];
        for (int i = 0; i < actual.length; i++) {
            getTypeParameters(member, types, argumentType, parameterTypes[i]);
        }
    }

    /**
     * Extract the method type parameters from the given type parameter and map
     * them to the actual parameters of the given argument.
     * 
     * @param member
     *            The method or constructor that defined the parameters.
     * @param types
     *            Type map of member type definitions to provided argument
     *            types.
     * @param parameterType
     *            Type type parameter defined by the member.
     * @param argumentType
     *            Type type argument given by the caller.
     */
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
                } else if (!Types.equals(argumentType, previous)) {
                    throw new IllegalArgumentException();
                }
            }
        } else if (parameterType instanceof WildcardType) {
            WildcardType wt = (WildcardType) parameterType;
            getTypeParameters(member, types, argumentType, wt.getLowerBounds());
        }
    }

    /**
     * Check that one type is assignable from another.
     * 
     * @param to
     *            The type to assign to.
     * @param from
     *            The type to assign from.
     * @exception IllegalArgumentException
     *                If the assignment cannot be made from the one type to the
     *                other.
     */
    private static void checkAssignable(Type to, Type from) {
        if (!getRawClass(to).isAssignableFrom(getRawClass(from))) {
            throw new IllegalArgumentException(format("Unable to assign [%s] from [%s].", getRawClass(to).getName(), getRawClass(from).getName()));
        }
    }

    /**
     * Create a new boxed instance of the type within the given key via the
     * given reflector.
     * 
     * @param reflector
     *            The reflector.
     * @param key
     *            The type to create.
     * @return A new boxed instance of the type.
     * @throws InstantiationException
     *             If the class is abstract, an interface, an array, or
     *             otherwise fails to construct.
     * @throws IllegalAccessException
     *             If either the class or its default constructor are
     *             inaccessible.
     */
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
            Type type = Types.getActualType(parameters[i], key.type, new LinkedList<Map<TypeVariable<?>,Type>>());
            type = Types.getActualType(type, assignments);
            if (!Types.isAssignableFrom(type, arguments[i].key.type)) {
                throw new IllegalArgumentException(format("Cannot assign [%s] from [%s].", parameters[i], arguments[i].key.type));
            }
        }
        return new Ilk.Box(key, reflector.newInstance(constructor, objects(arguments)));
    }

    /**
     * Create an array of unboxed object values from the given array of boxed
     * instances.
     * 
     * @param boxes
     *            The boxed instances.
     * @return An array of unboxed objects.
     */
    private static Object[] objects(Ilk.Box[] boxes) {
        Object[] objects = new Object[boxes.length];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = boxes[i].object;
        }
        return objects;
    }

    /**
     * Invoke a method on a boxed object instance with boxed instance parameters
     * returning a type safe boxed instance return value. This method will also
     * resolve method type variables using the argument types and use them
     * assign actual types to the method type variables in the return type. Each
     * of the method parameters is checked for assignability by checking that
     * the actual method parameter is assignable from the key of the boxed
     * argument. If any argument is not assignable to its parameter, an illegal
     * argument exception is raised.
     * 
     * @param reflector
     *            The reflector.
     * @param method
     *            The method.
     * @param object
     *            The boxed owner object or null for static methods.
     * @param arguments
     *            The boxed method arguments.
     * @return The return value as a boxed instance.
     * @throws IllegalAccessException
     *             If either the class or the method are inaccessible.
     * @throws InvocationTargetException
     *             If the method throws an exception during invocation.
     * @exception IllegalArgumentException
     *                If the number of parameters differ, of if the argument
     *                cannot be assigned to its parameter
     */
    public static Ilk.Box invoke(Reflector reflector, Method method, Ilk.Box object, Ilk.Box...arguments)
    throws IllegalAccessException, InvocationTargetException {
        checkAssignable(getRawClass(object.key.type), method.getDeclaringClass());
        Type[] parameters = method.getTypeParameters();
        Map<TypeVariable<?>, Type> assignments = new HashMap<TypeVariable<?>, Type>();
        for (int i = 0; i < parameters.length; i++) {
            getTypeParameters(method, assignments, parameters[i], arguments[i].key.type);
            Type actual = Types.getActualType(parameters[i], object.key.type, new LinkedList<Map<TypeVariable<?>,Type>>());
            actual = Types.getActualType(actual, assignments);
            if (!Types.isAssignableFrom(actual, arguments[i].key.type)) {
                throw new IllegalArgumentException(format("Cannot assign [%s] from [%s].", parameters[i], arguments[i].key.type));
            }
        }
        Object result = reflector.invoke(method, object.object, objects(arguments));
        Type actual = Types.getActualType(method.getGenericReturnType(), object.key.type, new LinkedList<Map<TypeVariable<?>,Type>>());
        actual = Types.getActualType(actual, assignments);
        return enbox(new Ilk.Key(actual), result);
    }

    /**
     * Set the field value of the given field using the given boxed instance as
     * the owner object to the given boxed instance value via the given
     * reflector. The object value is checked for assignability prior to
     * invocation and if the value is unassignable a
     * <code>IllegalArgumentExcpetion</code> is thrown.
     * 
     * @param reflector
     *            The reflector.
     * @param field
     *            The field.
     * @param object
     *            The boxed owner object or null for static fields.
     * @param value
     *            The new field value.
     * @throws IllegalAccessException
     *             If either the class or the field are inaccessible.
     */
    public static void set(Reflector reflector, Field field, Ilk.Box object, Ilk.Box value)
    throws IllegalAccessException {
        checkAssignable(field.getDeclaringClass(), getRawClass(object.key.type));
        Type actual = Types.getActualType(field.getGenericType(), object.key.type, new LinkedList<Map<TypeVariable<?>,Type>>());
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

    /**
     * Get the field value of the given field using the given boxed instance as
     * the owner object via the given relfector.
     * 
     * @param reflector
     *            The reflector.
     * @param field
     *            The field.
     * @param object
     *            The boxed owner object or null for static fields.
     * @return A boxed instance of the field value.
     * @throws IllegalAccessException
     *             If either the class or the field are inaccessible.
     */
    public static Ilk.Box get(Reflector reflector, Field field, Ilk.Box object)
    throws IllegalAccessException {
        checkAssignable(field.getDeclaringClass(), getRawClass(object.key.type));
        return enbox(new Ilk.Key(field.getGenericType()), reflector.get(field, object.object));
    }

    /** The default reflector instance. */
    public static final Reflector REFLECTOR = new Reflector();
    
    /**
     * Wrap calls to the Java reflection methods so that clients can
     * provide implementations that have access to package private classes
     * and members.
     *
     * @author Alan Gutierrez
     */
    public static class Reflector {
        /**
         * Create a new instance of the given type.
         * 
         * @param type
         *            Type type to create.
         * @return An instance of the type.
         * @throws InstantiationException
         *             If the class is abstract, an interface, an array, or
         *             otherwise fails to construct.
         * @throws IllegalAccessException
         *             If either the class or its default constructor are
         *             inaccessible.
         */
        public Object newInstance(Class<?> type)
        throws InstantiationException, IllegalAccessException {
            return type.newInstance();
        }

        /**
         * Create a new instance with the given constructor and the given
         * constructor arguments.
         * 
         * @param constructor
         *            The constructor.
         * @param arguments
         *            The constructor arguments.
         * @return An instance of the type.
         * @throws InstantiationException
         *             If the class is abstract.
         * @throws IllegalAccessException
         *             If either the class or its default constructor are
         *             inaccessible.
         * @throws InvocationTargetException
         *             If the constructor throws an exception during invocation.
         */
        public Object newInstance(Constructor<?> constructor, Object[] arguments)
        throws InstantiationException, IllegalAccessException, InvocationTargetException {
            return constructor.newInstance(arguments);
        }

        /**
         * Invoke the given method using the given owner object with the given
         * method arguments.
         * 
         * @param method
         *            The method.
         * @param object
         *            The owner object or null for static methods.
         * @param arguments
         *            The method arguments.
         * @return The method return value.
         * @throws IllegalAccessException
         *             If either the class or the method are
         *             inaccessible.
         * @throws InvocationTargetException
         *             If the method throws an exception during invocation.
         */
        public Object invoke(Method method, Object object, Object[] arguments)
        throws IllegalAccessException, InvocationTargetException {
            return method.invoke(object, arguments);
        }

        /**
         * Get the value of the given field using the given owner object.
         * 
         * @param field
         *            The field.
         * @param object
         *            The owner object or null for static fields.
         * @return The field value.
         * @throws IllegalAccessException
         *             If either the class or the field are
         *             inaccessible.
         */
        public Object get(Field field, Object object)
        throws IllegalAccessException {
            return field.get(object);
        }

        /**
         * Set the value of the given field using the given owner object.
         * 
         * @param field
         *            The field.
         * @param object
         *            The owner object or null for static fields.
         * @param value
         *            The new field value.
         * @throws IllegalAccessException
         *             If either the class or the field are
         *             inaccessible.
         */
        public void set(Field field, Object object, Object value)
        throws IllegalAccessException {
            field.set(object, value);
        }
    }
}

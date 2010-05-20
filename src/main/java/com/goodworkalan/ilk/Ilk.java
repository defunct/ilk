package com.goodworkalan.ilk;

import static com.goodworkalan.ilk.Types.getRawClass;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * An implementation of type tokens or Gafter's Gadget that generates a
 * navigable model of the parameterized types.
 * <p>
 * FIXME You can make diffused Ilk.Box by implementing an Ilk parser.
 * 
 * @author Alan Gutierrez
 * 
 * @param <T>
 *            The type to tokenize.
 */
public class Ilk<T> {
    /** The super type token key. */
    public final Key key;

    /**
     * Create a super type token for the given class.
     * 
     * @param keyClass
     *            The class.
     */
    public Ilk(Class<? extends T> keyClass) {
        this.key = new Key(keyClass);
    }

    /**
     * Create an ilk around a parameterized type.
     * 
     * @param parameterizedType
     *            The parameterized type.
     */
    public Ilk(ParameterizedType parameterizedType) {
        this.key = new Key(parameterizedType);
    }

    /**
     * Generate a super type token from the type parameters given in the class
     * type declaration.
     * <p>
     * This method is meant to be called from anonymous subclasses of
     * <code>Ilk</code>.
     */
    protected Ilk(Ilk.Key...keys) {
        // Give me class information.
        Class<?> klass = getClass();

        // See that I have created an anonymous subclass of TypeReference in the  
        // main method. Hence, to get the TypeReference itself, I need superclass.  
        // Furthermore, to get Type information, you should call  
        // getGenericSuperclass() instead of getSuperclass().  
        Type superClass = klass.getGenericSuperclass();  
       
        if (superClass instanceof Class<?>) {
            // Type has four subinterface:  
            // (1) GenericArrayType: component type is either a  
            // parameterized type or a type variable. Parameterized type is a class  
            // or interface with its actual type argument, e.g., ArrayList<String>.  
            // Type variable is unqualified identifier like T or V.  
            //  
            // (2) ParameterizedType: see (1).  
            //  
            // (3) TypeVariable<D>: see (1).  
            //  
            // (4) WildcardType: ?  
            //  
            // and one subclass:  
            // (5) Class.  
            //  
            // If TypeReference is created by 'new TypeReference() { }', then  
            // superClass would be just an instance of Class instead of one of the  
            // interfaces described above. In that case, because I don't have type  
            // passed to TypeReference, an exception should be raised.  
            throw new IllegalStateException("Missing Type Parameter");  
        }  
       
        // By superClass, we mean 'TypeReference<T>'. So, it is obvious that  
        // superClass is ParameterizedType.  
        ParameterizedType pt = (ParameterizedType) superClass;  
       
        // We have one type argument in TypeRefence<T>: T.
        Type type = pt.getActualTypeArguments()[0];
        if (type instanceof ParameterizedType) {
            key = new Ilk.Key((ParameterizedType) type, keys);  
        } else {
            if (keys.length != 0) {
                throw new IllegalArgumentException();
            }
            key = new Ilk.Key((Class<?>) type);
        }
    }

    /**
     * Create a key from the type which must be either a parameterized type or a
     * class.
     * 
     * @param type
     *            The type.
     * @param keys
     *            The type assignments for wildcards and type variables.
     * @return A key containing the type.
     * @return The type with the types replaced with the types represented by
     *         the list of keys.
     * @exception IllegalArgumentException
     *                If there are not enough or too many keys to replace the
     *                type parameters.
     * @exception ClassCastException
     *                If one of the keys cannot be assigned to the type.
     */
    static Key key(Type type, Key...keys) {
        if (type instanceof ParameterizedType) {
            return new Ilk.Key((ParameterizedType) type, keys);  
        }
        if (keys.length != 0) {
            throw new IllegalArgumentException();
        }
        return new Ilk.Key((Class<?>) type);
    }

    /**
     * Create a box that contains the given object that can return the given
     * object cast to the appropriate parameterized type using another ilk
     * instance.
     * 
     * @param object
     *            The object to box.
     * @return A box containing the object.
     */
    public Box box(T object) {
        return new Box(key, object);
    }

    public String toString() {
        return key.toString();
    }

    /**
     * Decorator of a Java class that tests assignability of type parameters.
     * 
     * @author Alan Gutierrez
     */
    public final static class Key implements Serializable, Comparable<Key> {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** The cached hash code for this key. */
        private final int hashCode;
        
        /** The type. */
        public final Type type;
        
        /** The raw type as a class. */
        public final Class<?> rawClass;

        /**
         * Actualize the given type by replacing the type variables with the
         * types in the given list of keys. If no keys are given, to replacement
         * is attempted, and the given type is wrapped.
         * 
         * @param type
         *            The type.
         * @param keys
         *            The list of keys.
         * @return The type with the types replaced with the types represented
         *         by the list of keys.
         * @exception IllegalArgumentException
         *                If there are not enough or too many keys to replace
         *                the type parameters.
         * @exception ClassCastException
         *                If one of the keys cannot be assigned to the type.
         */
        public Key(ParameterizedType type, Key...keys) {
            this.type = actualize(type, keys);
            this.rawClass = getRawClass(this.type);
            this.hashCode = makeHashCode(this.type);
        }
        
        public Key(Class<?> rawClass) {
            this.type = rawClass;
            this.rawClass = rawClass;
            this.hashCode = rawClass.hashCode();
        }

        /**
         * Actualize the given type by replacing the type variables with the
         * types in the given list of keys. If no keys are given, to replacement
         * is attempted, and the given type is wrapped.
         * 
         * @param type
         *            The type.
         * @param keys
         *            The list of keys.
         * @return The type with the types replaced with the types represented
         *         by the list of keys.
         * @exception IllegalArgumentException
         *                If there are not enough or too many keys to replace
         *                the type parameters.
         * @exception ClassCastException
         *                If one of the keys cannot be assigned to the type.
         */
        private static Type actualize(Type type, Key...keys) {
            if (keys.length == 0) {
                return type; 
            }
            Queue<Key> queue = new LinkedList<Key>(Arrays.asList(keys));
            Type actualized = actualize(type, null, queue);
            if (!queue.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return actualized;
        }

        /**
         * Check that the type variable given by to can be satisfied by the type
         * given by from.
         * 
         * @param type
         *            The type variable to assign.
         * @param assignment
         *            The value to assign.
         */
        private static void checkTypeVariable(Type type, Type assignment) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            for (Type bound : tv.getBounds()) {
                if (bound instanceof TypeVariable<?>) {
                    checkTypeVariable(bound, assignment);
                } else if (!isAssignableFrom(bound, assignment)) {
                    throw new IllegalArgumentException();
                }
            }
        }

        /**
         * Actualize the given type by replacing the type variables with the
         * types in the queue of keys. The queue of keys must contain enough
         * keys to satisfy all of the type variables and wildcards in the given
         * type and its type parameters and the type parameters of any
         * parameterized types in the type parameters. In other words, the queue
         * of keys must contain enough keys to satisfy all of the type variables
         * and wildcards the full type declaration of the given type.
         * 
         * @param type
         *            The type.
         * @param keys
         *            The list of keys.
         * @return The type with the types replaced with the types represented
         *         by the list of keys.
         * @exception IllegalArgumentException
         *                If there are not enough or too many keys to replace
         *                the type parameters.
         * @exception ClassCastException
         *                If one of the keys cannot be assigned to the type.
         */
        private static Type actualize(Type type, Type variable, Queue<Key> queue) {
            if (type instanceof ParameterizedType) {
                // If you ever feel the need to check the raw type arguments for
                // the parameterized type, don't bother. The type given is
                // assumed to be a correct actualization of the raw type.
                ParameterizedType pt = (ParameterizedType) type;
                Type[] arguments = pt.getActualTypeArguments();
                Type[] varaibles = getRawClass(pt).getTypeParameters();
                Type[] actualized = new Type[arguments.length];
                boolean dirty = false;
                for (int i = 0; i < arguments.length; i++) {
                    actualized[i] = actualize(arguments[i], varaibles[i], queue);
                    dirty = dirty || actualized[i] != arguments[i];
                }
                if (dirty) {
                    return new Types.ParameterizedType(pt, actualized);
                }
            } else if (type instanceof TypeVariable<?>) {
                // One cannot create a key without a class or parameterized
                // type, so we know that type in the key is will be an actual
                // type. It may have type parameters within it, however.
                Key key = take(queue);
                checkTypeVariable(type, key.type);
                return key.type;
            } else if (type instanceof WildcardType) {
                // One cannot create a key without a class or parameterized
                // type, so we know that type in the key is will be an actual
                // type. It may have type parameters within it, however.
                Key key = take(queue);
                if (!evaluateWildcards(type, key.type)) {
                    throw new ClassCastException();
                }
                checkTypeVariable(variable, key.type);
                return key.type;
            }
            return type;
        }

        /**
         * Take a key from the queue, throwing an illegal argument exception if
         * the queue is empty.
         * 
         * @param queue
         *            The queue.
         * @return The first key from the queue.
         * @exception IllegalArgumentException
         *                If the queue is empty.
         */
        private static Key take(Queue<Key> queue) {
            Key key = queue.poll();
            if (key == null) {
                throw new IllegalArgumentException();
            }
            return key;
        }

        /**
         * Using the given source type to lookup actual type parameters, resolve
         * the actual value of the given type variable.
         * 
         * @param source
         *            The source type to lookup actual type parameters.
         * @param typeVariable
         *            The type variable to actualize.
         * @return The actual parameterized type or class of the type variable.
         */
        private static Type getActualType(Method method, Map<TypeVariable<?>, Type> methodTypes, Type argument, Type derived, TypeVariable<?> typeVariable) {
            Type actualized = typeVariable;
            while (actualized instanceof TypeVariable<?>) {
                TypeVariable<?> tv = (TypeVariable<?>) actualized;
                if (tv.getGenericDeclaration().equals(method)) {
                    if (argument != null) {
                        checkTypeVariable(typeVariable, argument);
                        Type previous = methodTypes.get(typeVariable);
                        if (previous == null) {
                            methodTypes.put(typeVariable, argument);
                        } else if (!equals(argument, previous)) {
                            throw new IllegalArgumentException();
                        }
                        return argument;
                    }
                    return methodTypes.get(typeVariable);
                }
                Class<?> rawDeclarator = getRawClass((Type) tv.getGenericDeclaration());
                ParameterizedType actualDeclarator = (ParameterizedType) getSuperType(derived, rawDeclarator);
                Type[] parameters = rawDeclarator.getTypeParameters();
                for (int i = 0; ; i++)  {
                    if (parameters[i].equals(actualized)) {
                        actualized = actualDeclarator.getActualTypeArguments()[i];
                        break;
                    }
                }
            }
            return actualized;
        }

        /**
         * Get a key that encapsulates the actual type parameters of the given
         * super class or super interface of the raw class of this key.
         * 
         * @param keyClass
         *            The super class or super interface.
         * @return A key that encapsulates the actual type parameters of the
         *         given super class or null if the given key class is not a
         *         super class or super interface of the raw class of this key.
         */
        public Key getSuperKey(Class<?> keyClass) {
            return getKey(getSuperType(type, keyClass));
        }
        
        private Type[] getActualTypes(Method method, Map<TypeVariable<?>, Type> methodTypes, Type argumentType, Type[] types) {
            Type[] actual = new Type[types.length];
            boolean dirty = false;
            for (int i = 0; i < actual.length; i++) {
                actual[i] = getActualType(method, methodTypes, argumentType, types[i]);
                dirty = dirty || actual[i] != types[i];
            }
            return dirty ? actual : types;
        }

        private Type getActualType(Method method, Map<TypeVariable<?>, Type> methodTypes, Type argumentType, WildcardType wt) {
            Type[] lower = wt.getLowerBounds();
            Type[] actualLower = getActualTypes(method, methodTypes, argumentType, lower);
            Type[] upper = wt.getUpperBounds();
            Type[] actualUpper = getActualTypes(method, methodTypes, argumentType, upper);
            if (lower != actualLower || upper != actualUpper) {
                return new Types.WildcardType(actualLower, actualUpper);
            }
            return wt;
        }
        
        private Type getActualType(Method method, Map<TypeVariable<?>, Type> methodTypes, Type argumentType, Type genericType) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] types = pt.getActualTypeArguments();
                Type[] arguments = argumentType == null ? new Type[types.length] : ((ParameterizedType) argumentType).getActualTypeArguments();
                Type[] actual = new Type[types.length];
                boolean dirty = false;
                for (int i = 0; i < actual.length; i++) {
                    if (types[i] instanceof TypeVariable<?>) {
                        actual[i] = getActualType(method, methodTypes, arguments[i], type, (TypeVariable<?>) types[i]);
                    } else if (types[i] instanceof WildcardType){
                        actual[i] = getActualType(method, methodTypes, arguments[i], (WildcardType) types[i]);
                    } else {
                        actual[i] = types[i];
                    }
                    dirty = dirty || actual[i] != types[i];
                }
                return dirty ? new Types.ParameterizedType(pt, actual) : pt;
            } else if (genericType instanceof TypeVariable<?>) {
                return getActualType(method, methodTypes, argumentType, type, (TypeVariable<?>) genericType);
            }
            return genericType;
        }

        /**
         * Get a key that represents a class for the given type according to the
         * parameterized type definition.
         * 
         * @param find
         *            The type to resolve to an actual type.
         * @return The key for the actual type.
         * @exception IllegalArgumentException
         *                If the given type cannot be resolved to an actual
         *                type.
         */
        public Key getKey(Type find) {
            return key(getActualType(null, null, null, find));          
        }

        /**
         * Get a key that represents a class for the each of the given types
         * according to the parameterized type definition.
         * 
         * @param find
         *            The types to resolve to actual types.
         * @return The keys for the actual types.
         * @exception IllegalArgumentException
         *                If the given type cannot be resolved to an actual
         *                type.
         */
        public Key[] getKeys(Type[] find) {
            Key[] keys = new Key[find.length];
            for (int i = 0, stop = find.length; i < stop; i++) {
                keys[i] = getKey(find[i]);
            }
            return keys;
        }

        /**
         * Create a boxed new instance of type represented by this key using the
         * given constructor with the object values in the given list of boxes
         * as constructor arguments. Each constructor parameters is checked for
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
        public Ilk.Box newInstance(Reflect reflect, Constructor<?> constructor, Ilk.Box...arguments)
        throws InstantiationException, IllegalAccessException, InvocationTargetException { 
            return new Box(this, reflect.newInstance(constructor, objects(null, null, constructor.getGenericParameterTypes(), arguments)));
        }
        
        public Ilk.Box invoke(Method method, Ilk.Box object, Ilk.Box...arguments)
        throws IllegalAccessException, InvocationTargetException {
            if (!isAssignableFrom(object.key)) {
                throw new IllegalArgumentException();
            }
            Map<TypeVariable<?>, Type> methodTypes = new IdentityHashMap<TypeVariable<?>, Type>();
            Object[] objects = objects(method, methodTypes, method.getGenericParameterTypes(), arguments);
            return enbox(key(getActualType(method, methodTypes, null, method.getGenericReturnType())), method.invoke(object.object, objects));
        }
        
        public void set(Field field, Ilk.Box object, Ilk.Box value)
        throws IllegalAccessException {
            isAssignableFrom(object.key);
            getKey(field.getGenericType()).isAssignableFrom(value.key);
            field.set(object.object, value.object);
        }

        /**
         * Create a box for the given key and object or return null if the
         * object is null.
         * 
         * @param key
         *            The key.
         * @param object
         *            The object.
         * @return A box for the key and object or null if the object is null.
         */
        Ilk.Box enbox(Key key, Object object) {
            if (object == null) {
                return null;
            }
            return new Box(key, object);
        }

        public Ilk.Box get(Field field, Ilk.Box object)
        throws IllegalAccessException {
            isAssignableFrom(object.key);
            return enbox(getKey(field.getGenericType()), field.get(object.object));
        }

        public Object[] objects(Method method, Map<TypeVariable<?>, Type> methodTypes, Type[] types, Box[] boxes) {
            Key[] keys = new Key[types.length];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = key(getActualType(method, methodTypes, boxes[i].key.type, types[i]));
            }
            for (int i = 0; i < types.length; i++) {
                if (boxes[i] != null && !keys[i].isAssignableFrom(boxes[i].key)) {
                    throw new IllegalArgumentException();
                }
            }
            Object[] objects = new Object[boxes.length];
            for (int i = 0; i < objects.length; i++) {
                objects[i] = boxes[i] == null ? null : boxes[i].object;
            }
            return objects;
        }

        /**
         * Order keys by first by their assignability, where most derived types
         * are less than least derived types, then by their to string values.
         * Most derived types are less than their super types so that ordering
         * keys creates a series where the most derived type is earlier in the
         * series than its super types. The series can be used to find a best
         * match for a type where the most derived is considered the most
         * specific.
         * 
         * @return negative integer if this key is assignable to the given key,
         *         a positive integer if this key is assignable from the given
         *         key, or zero if they are both assignable to each other, or if
         *         no assignments are possible, the result of comparing the to
         *         string values.
         */
        public int compareTo(Key o) {
            if (isAssignableFrom(o)) {
                if (o.isAssignableFrom(this)) {
                    return 0;
                }
                return 1;
            } else if (o.isAssignableFrom(this)) {
                return -1;
            }
            return toString().compareTo(o.toString());
        }

        /**
         * Create a copy of this super type token.
         * 
         * @param key
         *            The key to copy.
         */
        public Key(Key key) {
            this.type = key.type;
            this.rawClass = key.rawClass;
            this.hashCode = key.hashCode;
        }

        /**
         * Using the given type declaration, find the definition of the given
         * target class in the class hierarchy of the raw class associated with
         * the type definition. Returns null if the target class is not part of
         * the class hierarchy of the type definition.
         * 
         * @param source
         *            The type definition.
         * @param target
         *            The target class.
         * @return The type definition of the class in the class hierarchy or
         *         null if it is not found.
         */
        private static Type getSuperType(Type source, Class<?> target) {
            if (getRawClass(source).equals(target)) {
                return source;
            }
            // Search either interfaces or classes, depending on the target. 
            if (target.isInterface()) {
                LinkedList<Type> interfaces = null;
                interfaces = new LinkedList<Type>();
                while (source != null) {
                    interfaces.addFirst(source);
                    while (!interfaces.isEmpty()) {
                        Class<?> candidate = getRawClass(interfaces.removeFirst());
                        for (Type type : candidate.getGenericInterfaces()) {
                            if (getRawClass(type).equals(target)) {
                                return type;
                            }
                        }
                        interfaces.addAll(Arrays.<Type>asList(candidate.getGenericInterfaces()));
                    }
                    source = getRawClass(source).getGenericSuperclass();
                }
            } else {
                for (;;) {
                    Type candidate = getRawClass(source).getGenericSuperclass();
                    if (candidate == null) {
                        break;
                    }
                    if (getRawClass(candidate).equals(target)) {
                        return candidate;
                    }
                    source = candidate;
                }
            }
            throw new IllegalArgumentException();
        }

        /**
         * For each element in the to array, if it is a wildcard, evaluate the
         * wildcard against the element at the same index in the from array,
         * otherwise test the element for equality against the same the element
         * at the same index in the from array.
         * 
         * @param to
         *            The types to assign to.
         * @param from
         *            The types to assign from.
         * @return True if the types in from can be assinged to the types in to.
         */
        private static boolean evaluateWildcards(Type[] to, Type[] from) {
            for (int i = 0, stop = to.length; i < stop; i++) {
                if (!evaluateWildcards(to[i], from[i])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * If the given to type is a wildcard, test the wildcard against the
         * given from type if it is not also a wildcard, otherwise test the two
         * types for equalty.
         * 
         * @param to
         *            The type to assign to.
         * @param from
         *            The type to assign from.
         * @return True if the types in from can be assinged to the types in to.
         */
        private static boolean evaluateWildcards(Type to, Type from) {
            if (to instanceof WildcardType && !(from instanceof WildcardType)) {
                WildcardType wt = (WildcardType) to;
                for (Type type : wt.getLowerBounds()) {
                    if (!isAssignableFrom(from, type)) {
                        return false;
                    }
                }
                for (Type type : wt.getUpperBounds()) {
                    if (!isAssignableFrom(type, from)) {
                        return false;
                    }
                }
                return true;
            }
            return equals(to, from);
        }

        /**
         * Determine of the type given in from can be assigned to type type
         * given in to.
         * 
         * @param to
         *            The type to assign to.
         * @param from
         *            The type to assign from.
         * @return True if the type from can be assigned to the type to.
         */
        private static boolean isAssignableFrom(Type to, Type from) {
            if (getRawClass(to).isAssignableFrom(getRawClass(from))) {
                if (to instanceof ParameterizedType) {
                    return equals(((ParameterizedType) to).getActualTypeArguments(), ((ParameterizedType) from).getActualTypeArguments());
                }
                return true;
            }
            return false;
        }

        /**
         * Determines if the class or interface represented by this key and all
         * parameterized types in the hierarchy of parameterized types is either
         * the same as, or is a superclass or super-interface of, the class or
         * interface of the class or interface in the same position in the
         * hierarchy represented by the given key. It returns <code>true</code>
         * if so; otherwise it returns <code>false</code>.
         * 
         * @param key
         *            The key to assign from.
         * @return True if the key can be assigned to an object represented by
         *         this key.
         */
        public boolean isAssignableFrom(Key key) {
            if (getRawClass(type).isAssignableFrom(getRawClass(key.type))) {
                if (type instanceof Class<?>) { 
                    return true;
                }
                Ilk.Key adjusted = key.getSuperKey(getRawClass(type));
                return evaluateWildcards(((ParameterizedType) type).getActualTypeArguments(), ((ParameterizedType) adjusted.type).getActualTypeArguments());
            }
            return false;
        }

        /**
         * Two keys are equal if the underlying types are equal. The underlying
         * types are equal if they are both classes and they are equal, or if
         * they are one of the other types and all of their properties are
         * equal.
         * 
         * @param object
         *            The object to test for equality.
         * @return True if this object is eqaual to the given object.
         */
        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof Key) {
                return equals(type, ((Key) object).type);
            }
            return false;
        }
        
        /**
         * Determine if the two arrays are the same length and if each element
         * in the left array is equal to the element at the same index in the
         * right array.
         * 
         * @param left
         *            One of two arrays to compare for equality.
         * @param right
         *            One of two arrays to compare for equality.
         * @return True if the arrays are equal.
         */
        private static boolean equals(Type[] left, Type[] right) {
            for (int i = 0, stop = left.length; i < stop; i++) {
                if (!equals(left[i], right[i])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Test two parameterized types to determine if they are equal. Two
         * parameterized types are equal if their owner types are equal or are
         * both null, their raw types are equal, their actual parameters arrays
         * are the same length and each element in the actual parameters array
         * is equal to the element in at the same index of the other actual
         * parameter array.
         * 
         * @param left
         *            One of two parameterized types to test for equality.
         * @param right
         *            One of two parameterized types to test for equality.
         * @return True if the parameterized types are equal.
         */
        private static boolean equals(ParameterizedType left, ParameterizedType right) {
            return equals(left.getRawType(), right.getRawType())
                && equals(left.getOwnerType(), right.getOwnerType())
                && equals(left.getActualTypeArguments(), right.getActualTypeArguments());
        }

        /**
         * Test two wildcard types to determine if they are equal. Two
         * parameterized types are equal the types of their upper and lower
         * bounds are equal. For now, we are only encountering arrays of bounds
         * with one element, but if we do encounter arrays of bounds with more
         * than one element, we'll need to implement set equality.
         * 
         * @param left
         *            One of two parameterized types to test for equality.
         * @param right
         *            One of two parameterized types to test for equality.
         * @return True if the parameterized types are equal.
         */
        private static boolean equals(WildcardType left, WildcardType right) {
            return equals(left.getLowerBounds(), right.getLowerBounds())
                && equals(left.getUpperBounds(), right.getUpperBounds());
        }

        /**
         * Determine if the two types are equal. Two types are equal if they are
         * the same class and all of their properties are equal.
         * <p>
         * Equality does not appear to be implemented for the reflection types,
         * so we implement it here.
         * 
         * @param left
         *            One of two types to compare.
         * @param right
         *            One of two types to compare.
         * @return True if the two types are equal.
         */
        private static boolean equals(Type left, Type right) {
            if (left == right) {
                return true;
            }
            if (left instanceof ParameterizedType) {
                if (right instanceof ParameterizedType) {
                    return equals((ParameterizedType) left, (ParameterizedType) right);
                }
            }
            if (left instanceof WildcardType) {
                if (right instanceof WildcardType) {
                    return equals((WildcardType) left, (WildcardType) right);
                }
            }
            return left.equals(right);
        }

        /**
         * Generate a hash code from the given parameterized type. Create a hash
         * code by combining the hash codes of the raw type, the owner type if
         * any, and the actual type parameters.
         * 
         * @param pt
         *            The parameterized type.
         * @return The hash code.
         */
        private static int makeHashCode(ParameterizedType pt) {
            return Arrays.asList(pt.getRawType(), pt.getOwnerType(), Arrays.asList(pt.getActualTypeArguments())).hashCode();
        }
        
        private static int makeHashCode(WildcardType wt) {
            return Arrays.<Object>asList(Arrays.asList(wt.getLowerBounds()), Arrays.asList(wt.getLowerBounds())).hashCode();
        }

        /**
         * Generate a hash code from the given type. The method will recursively
         * generate a hash code that combines the hash codes of the type and all
         * of the type parameters if the given type is a parameterized type.
         * 
         * @param type
         *            The type.
         * @return The hash code.
         */
        private static int makeHashCode(Type type) {
            if (type == null) {
                return 7;
            }
            if (type instanceof ParameterizedType) {
                return makeHashCode((ParameterizedType) type);
            }
            if (type instanceof WildcardType) {
                return makeHashCode((WildcardType) type);
            }
            return type.hashCode();
        }

        /**
         * Return a hash code that combines the hash code of the underlying type
         * which includes all of the type parameters if the underlying type is a
         * parameterized type.
         * 
         * @return The hash code.
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * Create a string that looks like the type declaration.
         * 
         * @return A string.
         */
        @Override
        public String toString() {
            if (type instanceof Class<?>) {
                return ((Class<?>) type).getName();
            }
            return type.toString();
        }
    }

    /**
     * A type-safe heterogeneous container for a single object that preserves
     * type information and safely casts a generic object back to its generic
     * type.
     * 
     * @author Alan Gutierrez
     */
    public final static class Box implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** The super type token key. */
        public final Key key;

        /** The object. */
        public final Object object;

        /**
         * Create a box that associates the given key with the given object.
         * 
         * @param key
         *            The key.
         * @param object
         *            The object.
         */
        Box(Key key, Object object) {
            this.key = key;
            this.object = object;
        }

        /**
         * Create a box that contains the given class and a key that represents
         * the given class with the class as the actual type.
         * <p>
         * I was able to create these before using reflection, but when I added
         * the wildcard boundary to the class constructor on ilk, it became
         * impossible to create through reflection. There was no way create anything
         * except the unbounded wildcard class.
         */
        public Box(Class<?> unboxedClass) {
            if (unboxedClass.getTypeParameters().length != 0) {
                throw new IllegalArgumentException();
            }
            this.key = new Key(new Types.ParameterizedType(Class.class, new Type[] { unboxedClass }));
            this.object = unboxedClass;
        }

        /**
         * Cast the given object to the given class.
         * 
         * @param <C>
         *            The type to cast to.
         * @param castClass
         *            The class to cast to.
         * @return The contained object cast to the class.
         * @exception ClassCastException
         *                If the object is not of the given type.
         */
        public <C> C cast(Class<C> castClass) {
            return cast(new Ilk<C>(castClass));
        }

        /**
         * Cast the given object to the given class.
         * <p>
         * This method generate the only unchecked cast warning in all of the
         * <code>Ilk</code> classes. Because we first check the assignability of
         * the key in the given <code>ilk</code> against the key property of
         * this <code>Ilk.Box</code>, we know that the unchecked cast is
         * actually safe, so we can suppress the unchecked warning.
         * 
         * @param <C>
         *            The type to cast to.
         * @param ilk
         *            The super type token of the type to cast to.
         * @return The contained object cast to the type.
         * @exception ClassCastException
         *                If the object is not of the given type.
         */
        @SuppressWarnings("unchecked")
        public <C> C cast(Ilk<C> ilk) {
            if (ilk.key.isAssignableFrom(key)) {
                return (C) object;
            }
            throw new ClassCastException();
        }
    }
    
    public static class Reflect {
        public Object invoke(Method method, Object object, Object[] arguments)
        throws IllegalAccessException, InvocationTargetException {
            return method.invoke(object, arguments);
        }
        
        public Object newInstance(Constructor<?> constructor, Object[] arguments)
        throws InstantiationException, IllegalAccessException, InvocationTargetException {
            return constructor.newInstance(arguments);
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

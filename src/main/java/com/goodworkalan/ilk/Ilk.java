package com.goodworkalan.ilk;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import java.util.List;

/**
 * An implementation of type tokens or Gafter's Gadget that generates a
 * navigable model of the parameterized types.
 *  
 * @author Alan Gutierrez
 *
 * @param <T> The type to tokenize.
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
    public Ilk(Class<T> keyClass) {
        this.key = new Key(keyClass);
    }

    /**
     * Generate a super type token from the type parameters given in the class
     * type declaration.
     * <p>
     * This method is meant to be called from anonymous subclasses of
     * <code>Ilk</code>.
     */
    protected Ilk(Key... keys) {
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
        key = new Ilk.Key(pt.getActualTypeArguments()[0], keys);  
    }

    /**
     * Create a box that contains the given object that can return the given
     * object cast to the appropriate parameterized type using a new ilk
     * instance.
     * 
     * @param object
     *            The object to box.
     * @return A box containing the object.
     */
    public Box box(T object) {
        return new Box(key, object);
    }

    /**
     * Get a list of constructors for the key class.
     * 
     * @return A list of constructors.
     */
    public List<Constructor<T>> getConstructors() {
        List<Constructor<T>> constructors = new ArrayList<Constructor<T>>();
        for (Constructor<?> constructor : key.bounds.get(0).boundaryClass.getConstructors()) {
            constructors.add(new UncheckedCast<Constructor<T>>().cast(constructor));
        }
        return constructors;
    }

    /**
     * A type parameter for a super type token. This type parameter includes the
     * parameter name plus a super type token key. It can be used to determine
     * the actual types of a parameterized type that only has wildcard type
     * information available.
     * 
     * @author Alan Gutierrez
     */
    public final static class Parameter implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /**
         * The parameter name that identifies this type definition in the type
         * parameter definitions of a parent key class.
         */
        public final String name;
        
        /** The super type tokens for this type definition. */
        public final Key key;
        
        /**
         * Create a parameter.
         * 
         * @param name
         *            The parameter name.
         * @param key
         *            The super type tokens for this type definition.
         */
        public Parameter(String name, Key key) {
            this.name = name;
            this.key = key;
        }

        /**
         * Create a deep copy of this parameter but with the new name. This will
         * create a copy of this parameter and the parameter type declarations
         * of the key referenced by this parameter.
         * 
         * @param parameter
         *            The parameter to copy.
         */
        public Parameter(Parameter parameter) {
            this(parameter.name, parameter.key);
        }

        /**
         * Create a copy of the parameter with the current name.
         * 
         * @param parameter
         *            The parameter to copy.
         * @param name
         *            The new name.
         */
        public Parameter(Parameter parameter, String name) {
            this(name, parameter.key);
        }

        /**
         * Compares the parameter to the given object. Returns true if the given
         * object is a parameter with a name and key value are equal to the
         * value name and key value of this parameter.
         * 
         * @param object
         *            An object to compare against this parameter.
         * @return True if the object is a parameter with a name and key value
         *         equal to that of this parameter.
         */
        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof Parameter) {
                Parameter parameter = (Parameter) object;
                return name.equals(parameter.name) && key.equals(parameter.key);
            }
            return false;
        }

        /**
         * Returns a hash code for this parameter. The hash code combines the
         * hash code of name and key of this parameter.
         * 
         * @return A hash code.
         */
        @Override
        public int hashCode() {
            int hashCode = 1;
            hashCode = hashCode * 37 + name.hashCode();
            hashCode = hashCode * 37 + key.hashCode();
            return hashCode;
        }
        
        // TODO Document.
        @Override
        public String toString() {
            return key.bounds.get(0).toString();
        }
    }

    /**
     * The wildcard types.
     * 
     * @author Alan Gutierrez
     */
    public enum Wildcard {
        /** The key is not parameterized. */
        NONE,
        /** The key is not a wildcard. */
        EXACT,
        /** The key is an upper wildcard specified by extends. */
        UPPER,
        /** The key is lower wildcard specified by super. */
        LOWER
    }
    
    public final static class Bound {
        /** The wildcard type. */
        public final Wildcard wildcard;

        /** The class of the boudary. */
        public final Class<?> boundaryClass;

        /**
         * Create a bound with the given boundary class and the given wildcard
         * type.
         * 
         * @param boundryClass
         *            The class of the boundary.
         * @param wildcard
         *            The wildcard type.
         */
        public Bound(Class<?> boundryClass, Wildcard wildcard) {
            this.boundaryClass = boundryClass;
            this.wildcard = wildcard;
        }
        
        public boolean isWithBounds(Class<?> from) {
            switch (wildcard) {
            case EXACT:
                return boundaryClass.equals(from);
            case NONE:
            case UPPER:
                return boundaryClass.isAssignableFrom(from);
            default:
                return from.isAssignableFrom(boundaryClass);
            }
        }

        /**
         * This object equals the given object if the given object is also a
         * bound and the boundary classes and wildcard are equal.
         * 
         * @param object
         *            The object to test for equality.
         * @return True if the object is equal to this object.
         */
        @Override
        public boolean equals(Object object) {
            if (object instanceof Bound) {
                Bound bound = (Bound) object;
                return boundaryClass.equals(bound.boundaryClass) && wildcard.equals(bound.wildcard);
            }
            return false;
        }

        /**
         * Generate a hash code as a combination of the hash code of the
         * boundary class hash code and the wildcard hash code.
         * 
         * @return The hash code.
         */
        @Override
        public int hashCode() {
            int hashCode = 1;
            hashCode = hashCode * 37 + boundaryClass.hashCode();
            hashCode = hashCode * 37 + wildcard.hashCode();
            return hashCode;
        }
        
        // TODO Document.
        @Override
        public String toString() {
            if (wildcard == Wildcard.LOWER) {
                return "? super " + boundaryClass.getCanonicalName();
            } else if (wildcard == Wildcard.UPPER) {
                return "? extends " + boundaryClass.getCanonicalName();
            }
            return boundaryClass.getCanonicalName();
        }
    }

    /**
     * Create an array from the variable argument list.
     * 
     * @param <T>
     *            The type of array.
     * @param objects
     *            The array elements.
     * @return An array containing the array elements.
     */
    static final<T> T[] array(T...objects) { 
        return objects;
    }
    
    /**
     * A navigable tree model of parameter type declaration. This class is a
     * structure containing the class and type parameters of a super type token
     * generated by creating a subclass of <code>Ilk</code>.
     * 
     * @author Alan Gutierrez
     */
    public final static class Key implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** The cached hash code for this key. */
        private final int hashCode;
        
        /** The type parameters. */
        public final List<Parameter> parameters;
        
        /** The type boundaries. */
        public final List<Bound> bounds;

        /**
         * Create a key for the given type.
         * <p>
         * The constructor will recursively create a key for each of the type
         * parameters of the type. It will calculate and cache the hash code,
         * since it is assumed that this class will primarily be used as a key
         * in map lookups.
         * 
         * @param type
         *            The type for which to generate a key.
         * @param keys
         *            Key queue.
         */
        public Key(Type type, Key... keys) {
            this(getBounds(type), getParameterKeys(type, null, toQueue(keys)));
        }
        
        // TODO Document.
        private static Queue<Key> toQueue(Key... keys) {
            return new LinkedList<Key>(Arrays.asList(keys));
        }

        /**
         * Construct a type key from the given type using the given {@link Ilk}
         * generated key to determine the actual type when wildcard types are
         * encountered in the given type.
         * 
         * @param type
         *            The type for which to generate a key.
         * @param key
         *            An {@link Ilk} generated key used to lookup actual types
         *            for wildcard types.
         */
        public Key(Key key, Type type) {
            this(getBounds(type), getParameterKeys(type, key, toQueue()));
        }

        private static Bound[] getBounds(Type type) {
            return array(new Bound(getKeyClass(type), Wildcard.NONE));
        }

        /**
         * Construct a key with the given key class, parameter name and type
         * parameter keys.
         * 
         * @param keyClass
         *            The key class.
         * @param parameterName
         *            The type parameter name of the key, if any.
         * @param parameters
         *            The parameter keys.
         */
        Key(Bound[] bounds, Parameter[] parameters) {
            this.bounds = Collections.unmodifiableList(Arrays.asList(bounds));
            this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
            this.hashCode = getHashCode();
        }

        /**
         * Generate a hash code by combining the hash code of the class of the
         * type token with the hash code of each of the type parameters of type
         * token.
         * 
         * @return The hash code.
         */
        private int getHashCode() {
            int hashCode = 1999;
            hashCode = hashCode * 37 + bounds.hashCode();
            hashCode = hashCode * 37 + parameters.hashCode();
            return hashCode;
       }

        /**
         * Return the class of the super type token type.
         * 
         * @param type
         *            The type.
         * @param parameter
         *            The type parameter definition in the parent class or null.
         * @return The class of the super type token.
         */
        private static Class<?> getKeyClass(Type type) {
            if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            } else if (type instanceof Class<?>) {
                return (Class<?>) type;
            }
            throw new IllegalArgumentException();
        }
        
        /**
         * Create an array of keys for the type parameters of the given type.
         * 
         * @param type
         *            The type.
         * @return An array of keys for the type parameters of the given type.
         */
        private static Parameter[] getParameterKeys(Type type, Key key, Queue<Key> queue) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Parameter[] parameters = new Parameter[pt.getActualTypeArguments().length];
                for (int i = 0; i < parameters.length; i++) {
                    Type actualType = pt.getActualTypeArguments()[i];
                    if (((actualType instanceof WildcardType) || (actualType instanceof TypeVariable<?>)) && key != null) {
                        Class<?> rawType = (Class<?>) pt.getRawType();
                        Class<?> keyType = key.bounds.get(0).boundaryClass;
                        if (rawType.isAssignableFrom(keyType)) {
                            Parameter parameter = null;
                            for (Type meta : keyType.getGenericInterfaces()) {
                                parameter = interfaces(i, key, rawType, meta);
                                if (parameter != null) {
                                    break;
                                }
                            }
                            if (parameter == null) {
                                parameter = superclasses(i, key, rawType, keyType.getGenericSuperclass());
                            }
                            if (parameter == null) {
                                throw new IllegalArgumentException();
                            }
                            parameters[i] = parameter;
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } else {
                        String parameterName = ((Class<?>) pt.getRawType()).getTypeParameters()[i].getName();
                        if (actualType instanceof TypeVariable<?>) {
                            if (queue.isEmpty())                            {
                                throw new IllegalArgumentException();
                            }
                            parameters[i] = new Parameter(parameterName, new Key(queue.poll()));
                        } else if (actualType instanceof WildcardType) {
                            Bound[] bounds = null;
                            WildcardType wt = (WildcardType) actualType;
                            if (wt.getLowerBounds() != null && wt.getLowerBounds().length != 0) {
                                bounds = array(new Bound(getKeyClass(wt.getLowerBounds()[0]), Wildcard.LOWER)); 
                            } 
                            if (wt.getUpperBounds() != null && wt.getUpperBounds().length != 0) {
                                Bound bound = new Bound(getKeyClass(wt.getUpperBounds()[0]), Wildcard.UPPER);
                                bounds = bounds == null ? array(bound) : array(bounds[0], bound);
                            } 
                            if (bounds == null) {
                                bounds = array(array(new Bound(getKeyClass(wt.getUpperBounds()[0]), Wildcard.NONE)));
                            }
                            parameters[i] = new Parameter(parameterName, new Key(bounds, new Parameter[0]));
                        } else {
                            parameters[i] = new Parameter(parameterName, new Key(array(new Bound(getKeyClass(actualType), Wildcard.EXACT)), getParameterKeys(actualType, key == null ? null : key.parameters.get(i).key, queue)));
                        }
                    }
                }
                return parameters;
            } else if (type instanceof Class<?>) {
                return new Parameter[0];
            } else if (type instanceof WildcardType) {
                return getParameterKeys(key.bounds.get(0).boundaryClass, key, queue);
            }
            throw new IllegalArgumentException();
        }

        // TODO Document.
        private static Parameter superclasses(int i, Key lookup, Class<?> rawType, Type meta) {
            if (meta == null) {
                return null;
            }
            if (meta instanceof ParameterizedType) {
                if (((ParameterizedType) meta).getRawType().equals(rawType)) {
                    Type actualType = ((ParameterizedType) meta).getActualTypeArguments()[i];
                    if (actualType instanceof TypeVariable<?>) {
                        String name = ((TypeVariable<?>) actualType).getName();
                        String newName = rawType.getTypeParameters()[i].getName();
                        return new Parameter(lookup.get(name), newName);
                    }
                }
                return superclasses(i, lookup, rawType, ((Class<?>) ((ParameterizedType) meta).getRawType()).getGenericSuperclass());
            }
            return null;
        }

        // TODO Document.
        private static Parameter interfaces(int i, Key lookup, Class<?> rawType, Type meta) {
            if (meta instanceof ParameterizedType) {
                if (((ParameterizedType) meta).getRawType().equals(rawType)) {
                    Type actualType = ((ParameterizedType) meta).getActualTypeArguments()[i];
                    if (actualType instanceof TypeVariable<?>) {
                        String name = ((TypeVariable<?>) actualType).getName();
                        String newName = rawType.getTypeParameters()[i].getName();
                        return new Parameter(lookup.get(name), newName);
                    }
                }
                for (Type subMeta : ((Class<?>) ((ParameterizedType) meta).getRawType()).getGenericInterfaces()) {
                    Parameter parameter = interfaces(i, lookup, rawType, subMeta);
                    if (parameter != null) {
                        return parameter;
                    }
                }
            }
            return null;
        }

        /**
         * Create a copy of this super type token.
         * 
         * @param key
         *            The key to copy.
         */
        public Key(Key key) {
            this.parameters = key.parameters;
            this.bounds = key.bounds;
            this.hashCode = key.hashCode;
        }
        
        /**
         * Return the key for a type parameter of the super type token declared
         * with the given parameter name.
         * 
         * @param name
         *            The name of the type parameter.
         * @return The key for the type parameter at the given index.
         */
        public Parameter get(String name) {
            for (Parameter parameter : parameters) {
                if (name.equals(parameter.name)) {
                    return parameter;
                }
            }
            return null;
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
            if (!isWithinBounds(key.bounds.get(0).boundaryClass)) {
                    return false;
            }
            if (parameters.size() != key.parameters.size()) {
                return false;
            }
            for (int i = 0, stop = parameters.size(); i < stop; i++) {
                Parameter parameter = parameters.get(i);
                Bound bound = key.parameters.get(i).key.bounds.get(0);
                if (!parameter.key.isWithinBounds(bound.boundaryClass)) {
                    return false;
                }
            }
            return true;
        }
        
        public boolean isWithinBounds(Class<?> boundaryClass) {
            for (Bound bound : bounds) {
                if (!bound.isWithBounds(boundaryClass)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Two keys are equal if the key classes are equal and the keys for each
         * of the type parameters at each position are equal. That is, both keys
         * have the name number of type parameters and each of the keys of the
         * type parameter in this key is equal to the key of type parameter of
         * the given key at the parallel position.
         * 
         * @param object
         *            The object to test for equality.
         */
        @Override
        public boolean equals(Object object) {
            if (object instanceof Key) {
                Key key = (Key) object;
                return bounds.equals(key.bounds) && parameters.equals(key.parameters);
            }
            return false;
        }

        /**
         * Return a hash code that combines the hash code of the key class with
         * the hash codes of each of the keys for the type parameters of the key
         * class.
         * 
         * @return The hash code.
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * Create a string representation of this super type token.
         * 
         * @return A string representation of this super type token.
         */
        @Override
        public String toString() {
            StringBuilder newString = new StringBuilder();
            newString.append(bounds.get(0).toString());
            if (parameters.size() != 0) {
                newString.append("<");
                String separator = "";
                for (Parameter parameter : parameters) {
                    newString.append(separator).append(parameter);
                    separator = ", ";
                }
                newString.append(">");
            }
            return newString.toString();
        }
    }
    
    // TODO Document.
    public final static class Box implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** The ilk key. */
        private final Key key;

        /** The object. */
        private final Object object;

        /**
         * Create a pair that associates the given key with the given object.
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
        
        public Box(Object object) {
            this.key = new Key(object.getClass());
            this.object = object;
        }

        /**
         * Get the ilk key.
         * 
         * @return The ilk key.
         */
        public Key getKey() {
            return key;
        }
        
        /**
         * Get the object.
         * 
         * @return The object.
         */
        public Object getObject() {
            return object;
        }

        // TODO Document.
        public <C> C cast(Ilk<C> ilk) {
            if (ilk.key.isAssignableFrom(key)) {
                return new UncheckedCast<C>().cast(object);
            }
            throw new ClassCastException();
        }
    }
}

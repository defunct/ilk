package com.goodworkalan.ilk;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
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
public class Ilk<T>
{
    /** The super type token key. */
    public final Key key;

    /**
     * Create a super type token for the given class.
     * 
     * @param keyClass
     *            The class.
     */
    public Ilk(Class<T> keyClass)
    {
        this.key = new Key(keyClass);
    }

    /**
     * Generate a super type token from the type parameters given in the class
     * type declaration.
     * <p>
     * This method is meant to be called from anonymous subclasses of
     * <code>Ilk</code>.
     */
    protected Ilk(Key... keys)
    {
        // Give me class information.  
        Class<?> klass = getClass();  
       
        // See that I have created an anonymous subclass of TypeReference in the  
        // main method. Hence, to get the TypeReference itself, I need superclass.  
        // Furthermore, to get Type information, you should call  
        // getGenericSuperclass() instead of getSuperclass().  
        Type superClass = klass.getGenericSuperclass();  
       
        if (superClass instanceof Class)
        {  
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
            throw new RuntimeException("Missing Type Parameter");  
        }  
       
        // By superClass, we mean 'TypeReference<T>'. So, it is obvious that  
        // superClass is ParameterizedType.  
        ParameterizedType pt = (ParameterizedType) superClass;  
       
        // We have one type argument in TypeRefence<T>: T.  
        key = new Ilk.Key(pt.getActualTypeArguments()[0], keys);  
    }
    
    // TODO Document.
    public Box box(T object)
    {
        return new Box(key, object);
    }
    
    /**
     * Get a list of constructors for the key class.
     * 
     * @return A list of constructors.
     */
    public List<Constructor<T>> getConstructors()
    {
        List<Constructor<T>> constructors = new ArrayList<Constructor<T>>();
        for (Constructor<?> constructor : key.getKeyClass().getConstructors())
        {
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
    public final static class Parameter implements Serializable
    {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** The parameter name. */
        private final String name;
        
        /** The super type token. */
        private final Key key;
        
        /**
         * Create a parameter.
         * 
         * @param name
         *            The parameter name.
         * @param key
         *            The super type token.
         */
        Parameter(String name, Key key)
        {
            this.name = name;
            this.key = key;
        }

        /**
         * Get the parameter name that identifies this type definition in the
         * type parameter definitions of a parent key class.
         * 
         * @return The parameter name of the key in the parent class.
         */
        public String getName()
        {
            return name;
        }

        /**
         * Get the super type token for this type definition.
         * 
         * @return The super type token.
         */
        public Key getKey()
        {
            return key;
        }
        
        /**
         * Create a deep copy of this parameter. This will create a copy
         * of this parameter and the parameter type declarations of the
         * key referenced by this parameter. 
         * 
         * @return A deep copy of this parameter.
         */
        public Parameter copy(String name)
        {
            return new Parameter(name, key.copy());
        }
        
        // TODO Document.
        public Parameter copy()
        {
            return copy(name);
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
        public boolean equals(Object object)
        {
            if (object == this)
            {
                return true;
            }
            if (object instanceof Parameter)
            {
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
        public int hashCode()
        {
            int hashCode = 1;
            hashCode = hashCode * 37 + name.hashCode();
            hashCode = hashCode * 37 + key.hashCode();
            return hashCode;
        }
        
        // TODO Document.
        @Override
        public String toString()
        {
            return key.toString();
        }
    }

    // TODO Document.
    public enum Wildcard
    {
        NONE, UPPER, LOWER
    }

    /**
     * A navigable tree model of parameter type declaration. This class is a
     * structure containing the class and type parameters of a super type token
     * generated by creating a subclass of <code>Ilk</code>.
     * 
     * @author Alan Gutierrez
     */
    public final static class Key implements Serializable
    {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** A flag for wildcards. */
        final Wildcard wildcard;
        
        /** The class of the object. */
        final Class<?> keyClass;
        
        /** The cached hash code for this key. */
        final int hashCode;
        
        /** A key for each type parameter of the class. */
        final Parameter[] parameters;

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
        public Key(Type type, Key... keys)
        {
            this(getKeyClass(type), getParameterKeys(type, null, toQueue(keys)), Wildcard.NONE);
        }

        
        public Wildcard getWildcard()
        {
            return wildcard;
        }

        // TODO Document.
        private static Queue<Key> toQueue(Key... keys)
        {
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
        public Key(Key key, Type type)
        {
            this(getKeyClass(type), getParameterKeys(type, key, toQueue()), Wildcard.NONE);
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
        private Key(Class<?> keyClass, Parameter[] parameters, Wildcard wildcard)
        {
            this.keyClass = keyClass;
            this.parameters = parameters;
            this.hashCode = getHashCode(keyClass.hashCode(), wildcard.hashCode(), parameters);
            this.wildcard = wildcard;
        }

        /**
         * Generate a hash code by combining the hash code of the class of the
         * type token with the hash code of each of the type parameters of type
         * token.
         * 
         * @param hashCode
         *            The hash code of the class of the type token.
         * @param parameters
         *            The type parameters of the type token.
         * @return A combined hash code.
         */
        public int getHashCode(int keyClass, int wildcard, Parameter[] parameters)
        {
            int hashCode = 1999;
            hashCode = hashCode * 37 + keyClass;
            hashCode = hashCode * 37 + wildcard;
            for (Parameter parameter : parameters)
            {
                hashCode = hashCode * 37 + parameter.hashCode(); 
            }
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
        private static Class<?> getKeyClass(Type type)
        {
            if (type instanceof ParameterizedType)
            {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            }
            else if (type instanceof Class)
            {
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
        private static Parameter[] getParameterKeys(Type type, Key key, Queue<Key> queue)
        {
            if (type instanceof ParameterizedType)
            {
                ParameterizedType pt = (ParameterizedType) type;
                Parameter[] parameters = new Parameter[pt.getActualTypeArguments().length];
                for (int i = 0; i < parameters.length; i++)
                {
                    Type actualType = pt.getActualTypeArguments()[i];
                    if (((actualType instanceof WildcardType) || (actualType instanceof TypeVariable)) && key != null)
                    {
                        Class<?> rawType = (Class<?>) pt.getRawType();
                        Class<?> keyType = key.getKeyClass();
                        if (rawType.isAssignableFrom(keyType))
                        {
                            Parameter parameter = null;
                            for (Type meta : keyType.getGenericInterfaces())
                            {
                                parameter = interfaces(i, key, rawType, meta);
                                if (parameter != null)
                                {
                                    break;
                                }
                            }
                            if (parameter == null)
                            {
                                parameter = superclasses(i, key, rawType, keyType.getGenericSuperclass());
                            }
                            if (parameter == null)
                            {
                                throw new IllegalArgumentException();
                            }
                            parameters[i] = parameter;
                        }
                        else
                        {
                            throw new IllegalArgumentException();
                        }
                    }
                    else
                    {
                        String parameterName = ((Class<?>) pt.getRawType()).getTypeParameters()[i].getName();
                        if ((actualType instanceof TypeVariable) && !queue.isEmpty()) 
                        {
                            if (!queue.isEmpty())
                            {
                                parameters[i] = new Parameter(parameterName, queue.poll().copy());
                            }
                            else
                            {
                                throw new IllegalArgumentException();
                            }
                        }
                        else if (actualType instanceof WildcardType)
                        {
                            // You're only able to declare one super or extends in
                            // a super type token.
                            WildcardType wt = (WildcardType) actualType;
                            if (wt.getUpperBounds() != null && wt.getUpperBounds().length != 0)
                            {
                                parameters[i] = new Parameter(parameterName, new Key((Class<?>) wt.getUpperBounds()[0], new Parameter[0], Wildcard.UPPER));
                            }
                            else if (wt.getLowerBounds() != null && wt.getLowerBounds().length != 0)
                            {
                                parameters[i] = new Parameter(parameterName, new Key((Class<?>) wt.getLowerBounds()[0], new Parameter[0], Wildcard.LOWER));
                            }
                            else
                            {
                                parameters[i] = new Parameter(parameterName, new Key(Object.class, new Parameter[0], Wildcard.UPPER));
                            }
                        }
                        else
                        {
                            parameters[i] = new Parameter(parameterName, new Key(getKeyClass(actualType), getParameterKeys(actualType, key == null ? null : key.get(i).getKey(), queue), Wildcard.NONE));
                        }
                    }
                }
                return parameters;
            }
            else if (type instanceof Class)
            {
                return new Parameter[0];
            }
            else if (type instanceof WildcardType)
            {
                return getParameterKeys(key.getKeyClass(), key, queue);
            }
            throw new IllegalArgumentException();
        }

        // TODO Document.
        private static Parameter superclasses(int i, Key lookup, Class<?> rawType, Type meta)
        {
            if (meta == null)
            {
                return null;
            }
            if (meta instanceof ParameterizedType)
            {
                if (((ParameterizedType) meta).getRawType().equals(rawType))
                {
                    Type actualType = ((ParameterizedType) meta).getActualTypeArguments()[i];
                    if (actualType instanceof TypeVariable)
                    {
                        String name = ((TypeVariable<?>) actualType).getName();
                        String newName = rawType.getTypeParameters()[i].getName();
                        return lookup.get(name).copy(newName);
                    }
                }
                return superclasses(i, lookup, rawType, ((Class<?>) ((ParameterizedType) meta).getRawType()).getGenericSuperclass());
            }
            return null;
        }

        // TODO Document.
        private static Parameter interfaces(int i, Key lookup, Class<?> rawType, Type meta)
        {
            if (meta instanceof ParameterizedType)
            {
                if (((ParameterizedType) meta).getRawType().equals(rawType))
                {
                    Type actualType = ((ParameterizedType) meta).getActualTypeArguments()[i];
                    if (actualType instanceof TypeVariable)
                    {
                        String name = ((TypeVariable<?>) actualType).getName();
                        String newName = rawType.getTypeParameters()[i].getName();
                        return lookup.get(name).copy(newName);
                    }
                }
                for (Type subMeta : ((Class<?>) ((ParameterizedType) meta).getRawType()).getGenericInterfaces())
                {
                    Parameter parameter = interfaces(i, lookup, rawType, subMeta);
                    if (parameter != null)
                    {
                        return parameter;
                    }
                }
            }
            return null;
        }
        
        /**
         * Get the class of the super type token.
         * 
         * @return The class of the super type token.
         */
        public Class<?> getKeyClass()
        {
            return keyClass;
        }

        /**
         * Create a copy of this super type token.
         * 
         * @return A copy of this super type token.
         */
        public Key copy()
        {
            Parameter copy[] = new Parameter[parameters.length];
            for (int i = 0; i < copy.length; i++)
            {
                copy[i] = parameters[i].copy();
            }
            return new Key(keyClass, copy, wildcard);
        }

        /**
         * Return the key for a type parameter of the super type token. Type
         * parameters are referenced by a zero based index and ordered according
         * to the their order in the class type parameter declaration.
         * 
         * @param index
         *            The index of the type parameter.
         * @return The key for the type parameter at the given index.
         */
        public Parameter get(int index)
        {
            return parameters[index];
        }
        
        /**
         * Return the key for a type parameter of the super type token declared
         * with the given parameter name.
         * 
         * @param name
         *            The name of the type parameter.
         * @return The key for the type parameter at the given index.
         */
        public Parameter get(String name)
        {
            for (Parameter parameter : parameters)
            {
                if (name.equals(parameter.getName()))
                {
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
        public boolean isAssignableFrom(Key key)
        {
            boolean assignable = keyClass.isAssignableFrom(key.keyClass)  && parameters.length == key.parameters.length;
            for (int i = 0; assignable && i < parameters.length; i++)
            {
                Key subKey = parameters[i].getKey();
                if (subKey.getWildcard() == Wildcard.LOWER)
                {
                    assignable = key.parameters[i].getKey().isAssignableFrom(subKey);
                }
                else
                {
                    assignable = subKey.isAssignableFrom(key.parameters[i].getKey());
                }
            }
            return assignable;
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
        public boolean equals(Object object)
        {
            if (object instanceof Key)
            {
                Key key = (Key) object;
                boolean equals = keyClass.equals(key.keyClass) && wildcard == key.wildcard && parameters.length == key.parameters.length;
                for (int i = 0; equals && i < parameters.length; i++)
                {
                    equals = parameters[i].equals(key.parameters[i]);
                }
                return equals;
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
        public int hashCode()
        {
            return hashCode;
        }
        
        /**
         * Create a string representation of this super type token.
         * 
         * @return A string representation of this super type token.
         */
        @Override
        public String toString()
        {
            StringBuilder newString = new StringBuilder();
            newString.append(keyClass.getName());
            if (parameters.length != 0)
            {
                newString.append("<");
                String separator = "";
                for (Parameter key : parameters)
                {
                    newString.append(separator).append(key);
                    separator = ", ";
                }
                newString.append(">");
            }
            return newString.toString();
        }
    }
    
    // TODO Document.
    public final static class Box implements Serializable
    {
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
        Box(Key key, Object object)
        {
            this.key = key;
            this.object = object;
        }
        
        /**
         * Get the ilk key.
         * 
         * @return The ilk key.
         */
        public Key getKey()
        {
            return key;
        }
        
        /**
         * Get the object.
         * 
         * @return The object.
         */
        public Object getObject()
        {
            return object;
        }

        // TODO Document.
        public <C> C cast(Ilk<C> ilk)
        {
            if (ilk.key.isAssignableFrom(key))
            {
                return new UncheckedCast<C>().cast(object);
            }
            throw new ClassCastException();
        }
    }
}

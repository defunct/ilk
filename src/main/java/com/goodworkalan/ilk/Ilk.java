package com.goodworkalan.ilk;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

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
    /** An empty queue. */
    private final static Queue<Key> EMPTY_QUEUE = new LinkedList<Key>();
    
    /** The super type token key. */
    public final Key key;

    // TODO Document.
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
    public Pair pair(T object)
    {
        return new Pair(key, object);
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
        public Parameter copy()
        {
            return new Parameter(name, key.copy());
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
            
            this(getKeyClass(type, null, EMPTY_QUEUE), getParameterKeys(type, null, toQueue(keys)));
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
            this(getKeyClass(type, null, EMPTY_QUEUE), getParameterKeys(type, key, toQueue()));
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
        private Key(Class<?> keyClass, Parameter[] parameters)
        {
            this.keyClass = keyClass;
            this.parameters = parameters;
            this.hashCode = getHashCode(keyClass.hashCode(), parameters);
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
        public int getHashCode(int hashCode, Parameter[] parameters)
        {
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
        private static Class<?> getKeyClass(Type type, Key subKey, Queue<Key> queue)
        {
            if (type instanceof ParameterizedType)
            {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            }
            else if (type instanceof Class)
            {
                return (Class<?>) type;
            }
            else if (type instanceof WildcardType) 
            {
                if (subKey != null)
                {
                    return subKey.getKeyClass();
                }
                else if (!queue.isEmpty())
                {
                    return queue.poll().getKeyClass();
                }
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
                    String parameterName = ((Class<?>) pt.getRawType()).getTypeParameters()[i].getName();
                    Key subKey = key == null ? null : key.get(parameterName).getKey();
                    Class<?> keyClass = getKeyClass(actualType, subKey, queue);
                    parameters[i] = new Parameter(parameterName, new Key(keyClass, getParameterKeys(actualType, null, queue)));
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
            return new Key(keyClass, getParameterKeys(keyClass, this, new LinkedList<Key>()));
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
                assignable = parameters[i].getKey().isAssignableFrom(key.parameters[i].getKey());
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
                boolean equals = keyClass.equals(key.keyClass) && parameters.length == key.parameters.length;
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
    public final static class Pair implements Serializable
    {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        // TODO Document.
        private final Key key;

        // TODO Document.
        private final Object object;
        
        // TODO Document.
        Pair(Key key, Object object)
        {
            this.key = key;
            this.object = object;
        }
        
        public Object getObject()
        {
            return object;
        }

        // TODO Document.
        public <C> C cast(Ilk<C> ilk)
        {
            if (ilk.key.equals(key))
            {
                return new UncheckedCast<C>().cast(object);
            }
            throw new ClassCastException();
        }
    }
}

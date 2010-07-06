package com.goodworkalan.ilk;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of type tokens or Gafter's Gadget that generates a
 * navigable model of the parameterized types.
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
     * Create a super type token for the given class representing a type that is
     * not generic.
     * 
     * @param type
     *            The class.
     */
    public Ilk(Class<? extends T> type) {
        this.key = new Key(type);
    }

    /**
     * Create a new <code>Ilk</code> by assigning the type variable represented
     * by the given <code>ilk</code> with the given <code>type</code>.
     * 
     * @param <V>
     *            The type variable type.
     * @param typeVariable
     *            The type variable super type token.
     * @param type
     *            The type.
     * @return A new super type token with the type variable replaced with the
     *         type.
     */
    public <V> Ilk<T> assign(TypeVariable<?> typeVariable, Type type) {
        Map<TypeVariable<?>, Type> types = new HashMap<TypeVariable<?>, Type>();
        types.put(typeVariable, type);
        Type assigned = Types.getActualType(key.type, types);
        return new Ilk<T>(assigned);
    }

    /**
     * Create an ilk around a parameterized type.
     * <p>
     * This method can really only be invoked via reflection, unless you get rid
     * of type parameters and are content to suppress warnings.
     * <p>
     * Look at where you're using this and come back and explain how this is not
     * cheating, not going to cause type warnings that occur during a hidden
     * cast. You are using it to create an Ilk that contains an Ilk.
     * 
     * @param type
     *            The parameterized type.
     */
    Ilk(Type type) {
        key = new Key(type);
    }
    
    /**
     * Generate a super type token from the type parameters given in the class
     * type declaration.
     * <p>
     * This method is meant to be called from anonymous subclasses of
     * <code>Ilk</code>.
     */
    protected Ilk() {
        // Give me class information.
        Class<?> klass = getClass();

        // See that I have created an anonymous subclass of TypeReference in the  
        // main method. Hence, to get the TypeReference itself, I need superclass.  
        // Furthermore, to get Type information, you should call  
        // getGenericSuperclass() instead of getSuperclass().  
        Type superClass = klass.getGenericSuperclass();  
       
//        if (superClass instanceof Class<?>) {
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
//            throw new IllegalStateException();  
//        }  
       
        // By superClass, we mean 'TypeReference<T>'. So, it is obvious that  
        // superClass is ParameterizedType.  
        ParameterizedType pt = (ParameterizedType) superClass;  
       
        // We have one type argument in TypeRefence<T>: T.
        key = new Key(pt.getActualTypeArguments()[0]);
    }

    /**
     * Create a box that contains the given object that can return the given
     * object cast to the appropriate parameterized type using an
     * <code>Ilk</code> instance.
     * 
     * @param object
     *            The object to box.
     * @return A box containing the object.
     */
    public Box box(T object) {
        return new Box(key, object);
    }

    /**
     * Generate a string representation of the <code>Ilk</code> by using the
     * string representation of the <code>Ilk.Key</code>.
     * 
     * @return A string representation of this object.
     */
    public String toString() {
        return key.toString();
    }
    
    /**
     * Decorator of a Java class that tests assignability of type parameters.
     * 
     * @author Alan Gutierrez
     */
    public final static class Key implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** The cached hash code for this key. */
        private final int hashCode;
        
        /** The type. */
        public final Type type;

        /**
         * Create a type key around the given type.
         * 
         * @param type
         *            The type.
         */
        public Key(Type type) {
            this.type = type;
            this.hashCode = Types.hashCode(type);
        }

        /**
         * Get a key for the type parameter at the given index.
         * 
         * @param index
         *            The type parameter index.
         * @return A key wrapping the type parameter.
         * @exception ClassCastException
         *                If the type is not a parameterized type.
         */
        public Key get(int index) {
            return new Key(((ParameterizedType) type).getActualTypeArguments()[index]);
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
            return Types.isAssignableFrom(type, key.type);
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
                return Types.equals(type, ((Key) object).type);
            }
            return false;
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
         * Otherwise, it is very difficult to create this class this type
         * contains itself.
         * 
         * @param classIlk
         *            The class.
         */
        public <T> Box(Ilk<Class<T>> classIlk) {
            this.key = classIlk.key;
            this.object = classIlk.key.type;
        }

        /**
         * Crate a boxed ilk of the given type.
         * 
         * @param <T>
         *            Type variable to satisfy compiler.
         * @param type
         *            The type.
         */
        public <T> Box(Type type) {
            this.key = new Key(new Types.Parameterized(Ilk.class, null, new Type[] { type }));
            this.object = new Ilk<T>(type);
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

        /**
         * Create a string representation of this <code>Ilk.Box</code> that that
         * gives the appearance of a map with one element whose key is the
         * <code>key</code> property an whose value is the <code>object</code>
         * property.
         * 
         * @return A string representation of this object.
         */
        public String toString() {
            return Collections.singletonMap(key, object).toString();
        }
    }
}

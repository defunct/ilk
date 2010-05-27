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
     * Create a super type token for the given class represnting a type that is
     * not generic..
     * 
     * @param keyClass
     *            The class.
     */
    public Ilk(Class<? extends T> keyClass) {
        this.key = new Key(keyClass);
    }

    /**
     * Create a new <code>Ilk</code> with a type variable assigned using the
     * given <code>Ilk</code>.
     * <p>
     * This method will assign a type contained in an <code>Ilk</code> type
     * using the given <code>ilk</code> that is itself described by the
     * <code>Ilk</code> given by <cdoe>ilkIlk</code>. The value of
     * 
     * <pre>
     * &lt;code&gt;
     * public &lt;T&gt; Ilk&lt;List&lt;T&gt;&gt; asList(Ilk&lt;T&gt; ilk) {
     *     return new Ilk&lt;List&lt;T&gt;&gt;(){}.assign(new Ilk&lt;Ilk&lt;T&gt;&gt;() {}, ilk);
     * }
     * 
     * public void example() {
     *     List&lt;Set&lt;String&gt;&gt; list = asList(new Ilk&lt;Set&lt;String&gt;&gt;() {});
     * }
     * &lt;/code&gt;
     * </pre>
     * 
     * This method will create new <code>Ilk</code> and is useful for creating
     * new type compositions in a type-safe manor.
     * 
     * @param <V>
     *            The type to assign.
     * @param ilkIlk
     *            An <code>Ilk</code> that describes the <code>Ilk</code> whose
     *            contained type will be used for the assignment.
     * @param ilk
     *            An <code>Ilk</code>
     * @return An <code>Ilk</code> with the type contained by the given
     *         <code>Ilk</code> assigned to every instance of the type variable
     *         used to represent the type parameter of the contained type.
     * @exception IllegalArgumentException
     *                If the contained type in the descriptive <code>Ilk</code>
     *                <code>ilkIlk</code> representing the type variable
     *                <code>V</code> it not itself a type variable.
     */
    public <V> Ilk<T> assign(Ilk<Ilk<V>> ilkIlk, Ilk<V> ilk) {
        Type typeVariable = ((ParameterizedType)ilkIlk.key.type).getActualTypeArguments()[0];
        if (!(typeVariable instanceof TypeVariable<?>)) {
            throw new IllegalArgumentException();
        }
        Map<TypeVariable<?>, Type> types = new HashMap<TypeVariable<?>, Type>();
        types.put((TypeVariable<?>) typeVariable, ilk.key.type);
        Type assigned = Types.getActualType(key.type, types);
        return new Ilk<T>(assigned);
    }
    
    /**
     * Create a new <code>Ilk</code> by assigning the type variable represented
     * by the given <code>ilk</code> with the given <code>type</code>. THe gi 
     * @param <V>
     * @param typeVariable
     * @param type
     * @return
     */
    public <V> Ilk<T> assign(Ilk<V> typeVariable, Type type) {
        Map<TypeVariable<?>, Type> types = new HashMap<TypeVariable<?>, Type>();
        types.put((TypeVariable<?>) typeVariable.key.type, type);
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
            throw new IllegalStateException();  
        }  
       
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
     * It is also rather annoying to get an <code>Ilk.Box</code> with an
     * <code>Ilk</code> in it, so here is a method to do that.
     * 
     * @return This Ilk boxed.
     */
    public Box box() {
        return new Box(new Key(new Types.Parameterized(Ilk.class, null, new Type[] { key.type })), this);
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
     * <p>
     * FIXME I do not need all this in this class. I can simply assert that an
     * ilk key is built around a parameterized type or a raw class. All of the
     * reflection, all of the replacement, can be moved to external classes.
     * <p>
     * It is important to keep his minimal so that something like Stash does not
     * become 32K library.
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
         * Actualize the given type by replacing the type variables with the
         * types in the given list of keys. If no keys are given, to replacement
         * is attempted, and the given type is wrapped.
         * 
         * @param type
         *            The type.
         * @param keys
         *            The list of keys.
         * @exception IllegalArgumentException
         *                If there are not enough or too many keys to replace
         *                the type parameters.
         * @exception ClassCastException
         *                If one of the keys cannot be assigned to the type.
         */
        public Key(Type type) {
            this.type = Types.getEquatable(type);
            this.hashCode = type.hashCode();
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
                return type.equals(((Key) object).type);
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
     * <p>
     * XXX Hey! Here is a place where you favored immutability, all the way
     * down the line.
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
         */
        public <T> Box(Ilk<Class<T>> classIlk) {
            this.key = classIlk.key;
            this.object = classIlk.key.type;
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

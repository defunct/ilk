package com.goodworkalan.ilk;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Ilk<T>
{
    public final Key key;
    
    public Ilk()
    {
        // Give me class information.  
        Class<?> klass = getClass();  
       
        // See that I have created an anonymous subclass of TypeReference in the  
        // main method. Hence, to get the TypeReference itself, I need superclass.  
        // Furthermore, to get Type information, you should call  
        // getGenericSuperclass() instead of getSuperclass().  
        java.lang.reflect.Type superClass = klass.getGenericSuperclass();  
       
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
        key = new Ilk.Key(pt.getActualTypeArguments()[0]);  
    }
    
    public final static class Key implements Serializable
    {
        private static final long serialVersionUID = 1L;

        final Class<?> keyClass;
        
        final int hashCode;
        
        final Key[] parameters;
        
        public Key(Type type)
        {
            this.keyClass = getKeyClass(type);
            this.parameters = getParameterKeys(type);
            this.hashCode = getHashCode(keyClass.hashCode(), parameters);
        }
        
        public int getHashCode(int hashCode, Key[] parameters)
        {
            for (Key key : parameters)
            {
                hashCode = hashCode * 37 + key.hashCode(); 
            }
            return hashCode;
        }
        
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
        
        private Key[] getParameterKeys(Type type)
        {
            if (type instanceof ParameterizedType)
            {
                ParameterizedType pt = (ParameterizedType) type;
                Key[] parameters = new Key[pt.getActualTypeArguments().length];
                for (int i = 0; i < parameters.length; i++)
                {
                    parameters[i] = new Key(pt.getActualTypeArguments()[i]);
                }
                return parameters;
            }
            else if (type instanceof Class)
            {
                return new Key[0];
            }
            throw new IllegalArgumentException();
        }
        
        public Class<?> getKeyClass()
        {
            return keyClass;
        }

        public Key get(int index)
        {
            return parameters[index];
        }
        
        @Override
        public boolean equals(Object object)
        {
            if (object instanceof Key)
            {
                Key key = (Key) object;
                boolean equals = keyClass.equals(key.keyClass);
                for (int i = 0; equals && i < parameters.length; i++)
                {
                    equals = parameters[i].equals(key.parameters[i]);
                }
                return equals;
            }
            return false;
        }
        
        @Override
        public int hashCode()
        {
            return hashCode;
        }
    }
}

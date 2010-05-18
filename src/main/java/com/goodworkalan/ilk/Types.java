package com.goodworkalan.ilk;

import java.lang.reflect.Type;

/**
 * Utility methods and nested classes to support the {@link Ilk} implementation.
 * <p>
 * The nested implementation of {@link java.lang.reflect.ParameterizedType}
 * allows the reuse of the name (instead of adding "Impl"), while keeping
 * qualification terse, simply prepend "Types."
 * 
 * @author Alan Gutierrez
 */
class Types {
    /**
     * Return the class of the super type token type.
     * 
     * @param type
     *            The type.
     * @return The class of the super type token.
     */
    public static Class<?> getRawClass(Type type) {
        if (type instanceof java.lang.reflect.ParameterizedType) {
            return (Class<?>) ((java.lang.reflect.ParameterizedType) type).getRawType();
        }
        return (Class<?>) type;
    }

    /**
     * Implementation of {@link java.lang.reflect.ParameterizedType} in order to
     * provide actualized type arguments.
     * 
     * @author Alan Gutierrez
     */
    public static class ParameterizedType implements java.lang.reflect.ParameterizedType {
        /** The owner type. */
        private final Type ownerType;
        
        /** The raw type. */
        private final Type rawType;
        
        /** The actual type arguments. */
        private final Type [] actualTypeArguments;

        /**
         * Create a parameterized type from the given parameterized type with
         * the given type arguments in lieu of the type arguments in the given
         * parameterized type.
         * 
         * @param pt
         *            The parameterized type.
         * @param actualTypeArguments
         *            The replacement actual type arguments.
         */
        public ParameterizedType(java.lang.reflect.ParameterizedType pt, Type[] actualTypeArguments) {
            this.ownerType = pt.getOwnerType();
            this.rawType = pt.getRawType();
            this.actualTypeArguments = actualTypeArguments;
        }

        /**
         * Get an array of <tt>Type</tt> objects representing the actual type
         * arguments to this type.
         * 
         * @return The actual type arguments.
         */
        public Type[] getActualTypeArguments() {
            return actualTypeArguments.clone();
        }

        /**
         * Get a <code>Type</code> object representing the type that this type
         * is a member of.
         * 
         * @return The owner type.
         */
        public Type getOwnerType() {
            return ownerType;
        }

        /**
         * Get the <tt>Type</tt> object representing the class or interface that
         * declared this type.
         * 
         * @return The raw type.
         */
        public Type getRawType() {
            return rawType;
        }
        
        /**
         * Create a string representation that resembles the type declaration.
         * 
         * @return The string representation.
         */
        @Override
        public String toString() {
            StringBuilder string = new StringBuilder();
            string.append(((Class<?>) rawType).getName()).append("<");
            String separator = "";
            for (Type type : actualTypeArguments) {
                string.append(separator);
                if (type instanceof Class<?>) {
                    string.append(((Class<?>) type).getName());
                } else {
                    string.append(type);
                }
                separator = ", ";
            }
            string.append(">");
            return string.toString();
        }
    }
}

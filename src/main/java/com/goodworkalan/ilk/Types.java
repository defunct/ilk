package com.goodworkalan.ilk;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Utility methods and nested classes to support the {@link Ilk} implementation.
 * <p>
 * The nested implementation of {@link java.lang.reflect.ParameterizedType}
 * allows the reuse of the name (instead of adding "Impl"), while keeping
 * qualification terse, simply prepend "Types."
 * 
 * @author Alan Gutierrez
 */
public class Types {
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
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }

    /**
     * Get an array of the methods in the type that have the given name.
     * 
     * @param type
     *            Type class.
     * @param name
     *            The method name.
     * @return An array of methods in the type that have the given name.
     */
    public static Method[] getMethods(Class<?> type, String name) {
        ArrayList<Method> methods = new ArrayList<Method>();
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                methods.add(method);
            }
        }
        return methods.toArray(new Method[methods.size()]);
    }

    /**
     * Converts the given <code>Type</code> into a string representation using
     * the class name instead of the <code>toString</code> value if the
     * <code>Type</code> is a <code>Class</code>.
     * 
     * @param type
     *            The type.
     * @return A string representation of the type.
     */
    static String typeToString(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getName();
        }
        return type.toString();
    }

    /**
     * An implementation of {@link java.lang.reflect.WildcardType WildcardType}
     * to allow for the creation of <code>WildcardType</code> instances where
     * <code>TypeVariable</code> instances have been replaced with
     * <code>Class</code> or <code>ParameterizedType</code> instances.
     * 
     * @author Alan Gutierrez
     */
    public static class Wildcard implements java.lang.reflect.WildcardType {
        /** The sub most interface implemented by this wildcard type. */
        private final Type[] lowerBounds;

        /** The super most interfaces implemented by this wildcard type. */
        private final Type[] upperBounds;

        /**
         * Create a wild card type with the given upper and lower bounds.
         * 
         * @param lowerBounds
         *            The sub most interface implemented by this wildcard type.
         * @param upperBounds
         *            The super most interfaces implemented by this wildcard
         *            type.
         */
        public Wildcard(Type[] lowerBounds, Type[] upperBounds) {
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
        }

        /**
         * Get a copy of the sub most interfaces required by this wildcard type.
         * 
         * @return The sub most interfaces required by this wildcard type.
         */
        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        /**
         * Get a copy of the super most interfaces implemented by this wildcard
         * type.
         * 
         * @return The super most interfaces implemented by this wildcard type.
         */
        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }  

        /**
         * Create a string representation of the <code>WildcardType</code> as it
         * would appear in Java source code.
         * 
         * @return A string representation of this <code>WildcardType</code>.
         */
        public String toString() {
            StringBuffer string = new StringBuffer();
            if (lowerBounds.length != 0) { 
                string.append("? super ").append(typeToString(lowerBounds[0]));
            } else {
                string.append("? extends ");
                String separator = "";
                for (Type upper : upperBounds) {
                    string.append(separator).append(typeToString(upper));
                    separator = " & ";
                }
            }
            return string.toString();
        }
    }

    /**
     * Implementation of {@link java.lang.reflect.ParameterizedType} in order to
     * provide actualized type arguments.
     * <p>
     * The <code>hashCode</code> method for <code>ParameterizedType</code> is
     * undocumented. To actualize types, we'll need to build our own <code>ParameterizedType</code>,
     * and to use something for a key, we'll always have to replace all of the <code>ParameterizedType</code>
     * nodes in a tree. Thus, this type is only equal to the exact type, not 
     * to any <code>ParameterizedType</code>.
     * 
     * @author Alan Gutierrez
     */
    public static class Parameterized implements ParameterizedType {
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
         * @param rawType
         *            The raw type.
         * @param ownerType
         *            The owner type
         * @param actualTypeArguments
         *            The replacement actual type arguments.
         */
        public Parameterized(Type rawType, Type ownerType, Type[] actualTypeArguments) {
            this.ownerType = ownerType;
            this.rawType = rawType;
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
        public String toString() {
            StringBuilder string = new StringBuilder();
            string.append(((Class<?>) rawType).getName()).append("<");
            String separator = "";
            for (Type type : actualTypeArguments) {
                string.append(separator).append(typeToString(type));
                separator = ", ";
            }
            string.append(">");
            return string.toString();
        }
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
    public static boolean isParameterAssignableFrom(Type to, Type from) {
        if (getRawClass(to).isAssignableFrom(getRawClass(from))) {
            if (to instanceof ParameterizedType) {
                return equals(((ParameterizedType) to).getActualTypeArguments(), ((ParameterizedType) from).getActualTypeArguments());
            }
            return true;
        }
        return false;
    }

    /**
     * Create a map of type variables to their assigned types for the given
     * actualized type. This will create a map that has an assignment for every
     * type variable declared by every super class and every interface
     * implemented by the given actualized type.
     * <p>
     * Type maps should not be combined. Don't attempt to build a type map that
     * contains the type variable assignments for more than one type. Don't
     * attempt to combine the type maps of a type and its owner type, for
     * example. If two types implement the same generic super class or
     * interface, the subsequent assignments for the implemented type will
     * overwrite in the initial assignments.
     * 
     * @param types
     *            The map of type variables to their assigned types.
     * @param source
     *            The actualized type.
     */
    public static void getHierarchTypes(Map<TypeVariable<?>, Type> types, Type source) {
        if (source != null) {
            if (source instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) source;
                Type[] arguments = pt.getActualTypeArguments();
                TypeVariable<?>[] parameters = getRawClass(source).getTypeParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Type assignment = null;
                    if (arguments[i] instanceof TypeVariable<?>) {
                        assignment = types.get(arguments[i]);
                    }
                    types.put(parameters[i], assignment == null ? arguments[i] : assignment);
                }
            }
            for (Type iface : getRawClass(source).getGenericInterfaces()) {
                getHierarchTypes(types, iface);
            }
            getHierarchTypes(types, getRawClass(source).getGenericSuperclass());
        }
    }

    /**
     * Determine if one type is assignable from another type. The types must be
     * instances of <code>Class</code> or <code>ParameterizedType</code>.
     * 
     * @param to
     *            The type to assign to.
     * @param from
     *            The type to assign from.
     * @return True if type to assign form can be assigned to the type to assign
     *         to.
     */
    public static boolean isAssignableFrom(Type to, Type from) {
        if (getRawClass(to).isAssignableFrom(getRawClass(from))) {
            if (to instanceof Class<?>) { 
                return true;
            }
            ParameterizedType actualFrom = (ParameterizedType) getActualType(getRawClass(to), from, new LinkedList<Map<TypeVariable<?>, Type>>());
            Type[] typesTo = ((ParameterizedType) to).getActualTypeArguments();
            Type[] typesFrom = actualFrom.getActualTypeArguments();
            for (int i = 0; i < typesTo.length; i++) {
                int code = typeAsCode(typesTo[i]);
                if (code == 3 && code != typeAsCode(typesFrom[i])) {
                    if (!checkWildcardType((WildcardType) typesTo[i], typesFrom[i], false)) {
                        return false;
                    }
                } else if (!equals(typesTo[i], typesFrom[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Check that the given assignment can be assigned to the wildcard type, or
     * vice-versa if the flip parameter is true.
     * 
     * @param wt
     *            The wildcard type.
     * @param assignment
     *            The assignment.
     * @param flip
     *            Whether to flip the test.
     * @return True if the assignment can be assigned to the wildcard type.
     */
    public static boolean checkWildcardType(WildcardType wt, Type assignment, boolean flip) {
        Type[] lower = wt.getLowerBounds();
        for (int i = 0; i < lower.length; i++) {
            if (!isParameterAssignableFrom(assignment, lower[i])) {
                return false;
            }
        }
        Type[] upper = wt.getUpperBounds();
        for (int i = 0; i < upper.length; i++) {
            if (upper[i] instanceof TypeVariable<?>) {
                TypeVariable<?> upperTypeVariable = (TypeVariable<?>) upper[i];
                for (Type otherBound : upperTypeVariable.getBounds()) {
                    if (!isParameterAssignableFrom(flip ? otherBound : assignment,  flip ? assignment : otherBound)) {
                        return false;
                    }
                }
            } else if (!isParameterAssignableFrom(flip ? assignment : upper[i], flip ? upper[i] : assignment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check that the given assignment can be assigned to the type variable by
     * checking the type variables bounds throwing an
     * <code>IllegalArgumentException</code> if the type cannot be assigned.
     * 
     * @param type
     *            The type variable.
     * @param assignment
     *            The assignment.
     */
    public static void checkTypeVariable(Type type, Type assignment) {
        TypeVariable<?> tv = (TypeVariable<?>) type;
        for (Type bound : tv.getBounds()) {
            if (bound instanceof TypeVariable<?>) {
                checkTypeVariable(bound, assignment);
            } else if (bound instanceof ParameterizedType) {
//                if (!isAssignableFrom(getActualType(bound, assignments))) {
//                    throw new IllegalArgumentException();
//                }
            } else if (assignment instanceof WildcardType) {
                if (!checkWildcardType((WildcardType) assignment, bound, true)) { 
                    throw new IllegalArgumentException();
                }
            } else if (!isAssignableFrom(bound, assignment)) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Create an actual sub-type of the given type replacing variables with the
     * types in the given variable map.
     * 
     * @param unactualized
     *            The type whose type variables will be assigned types from the
     *            type variable map.
     * @param assignments
     *            The map of type variables to actual variables to map types
     *            that are actualized in the type hierarchy.
     * @return The actual type.
     */
    public static Type getActualType(Type unactualized, Map<TypeVariable<?>, Type> assignments) {
        if (unactualized == null || (unactualized instanceof GenericArrayType)) {
            return unactualized;
        }
        if (unactualized instanceof WildcardType) {
            WildcardType wt = (WildcardType) unactualized;
            Type[] lower = wt.getLowerBounds();
            for (int i = 0; i < lower.length; i++) {
                lower[i] = getActualType(lower[i], assignments);
            }
            Type[] upper = wt.getUpperBounds();
            for (int i = 0; i < upper.length; i++) {
                upper[i] = getActualType(upper[i], assignments);
            }
            return new Types.Wildcard(lower, upper);
        } 
        if (unactualized instanceof TypeVariable<?>) {
            Type actual = assignments.get(unactualized);
            if (actual == null) {
                return unactualized;
            }
//            actual = getActualType(actual, assignments);
            // FIXME Yes, you can replace TypeVariable with TypeVariable, so check.
            if (!(actual instanceof TypeVariable<?>)) {
                checkTypeVariable(unactualized, actual);
            }
            return actual;
        }
        if (unactualized instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) unactualized;
            Type[] parameters = pt.getActualTypeArguments();
            Type[] actual = new Type[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                actual[i] = getActualType(parameters[i], assignments);
            }
            Type ownerType = pt.getOwnerType();
            if (ownerType != null && ((ownerType instanceof Class<?>) || (ownerType instanceof ParameterizedType))) {
                ownerType = getActualType(ownerType, assignments);
            }
            return new Types.Parameterized(getRawClass(unactualized), ownerType, actual);
        }
        TypeVariable<?>[] variables = getRawClass(unactualized).getTypeParameters();
        if (variables.length == 0) {
            return getRawClass(unactualized);
        }
        Type[] actual = new Type[variables.length];
        for (int i = 0; i < variables.length; i++) {
            actual[i] = getActualType(variables[i], assignments);
        }
        // Bogus temporary owner type and the null is very important.
        return new Types.Parameterized(getRawClass(unactualized), null, actual);
    }

    /**
     * Create an actual type for the given unactualized type by replacing the
     * type variables defined by the type, its super classes, and implemented
     * interfaces with the type variable assignments defined by the actual type.
     * <p>
     * The type assignments for all of the super classes and implemented
     * interfaces of the actualized type are determined by navigating its
     * hierarchy from the lowest bound, to each of the upper bounds populating a
     * map with type variable assignments. Type variables that are assigned type
     * variables lookup the actual type in the map as it goes by.
     * <p>
     * To create an actualized type from the unactualized type a type map is
     * created for the actualized type, and the types from the actualized type
     * are applied to the unactualized type. Thereafter, if the actualized type
     * is a nested type, then a type variable map is created for the owner type,
     * an type assignments defined by the map are applied to the partially
     * actualized type. This step is repeated if the parent is also a nested
     * class, until an ancestor that is not a nested class is encountered.
     * <p>
     * The linked list of type variable assignment maps is used to create a list
     * of the type variable assignment maps for a nested types and their parent
     * types.
     * 
     * @param unactualized
     *            The unactualized type.
     * @param actualized
     *            The actualized type.
     * @param assignments
     *            An empty linked list of type variable assignment maps.
     * @return An actual type created by replacing the type variables of the
     *         unactualized type with the type variable assignments of the
     *         actualized type.
     */
    public static Type getActualType(Type unactualized, Type actualized, LinkedList<Map<TypeVariable<?>, Type>> assignments) {
        Type ownerType = null;
        Class<?> rawClass = getRawClass(unactualized);
        if (rawClass != null && rawClass.getDeclaringClass() != null) {
            Type actualizedOwner = null;
            if (actualized instanceof ParameterizedType) {
                actualizedOwner = ((ParameterizedType) actualized).getOwnerType();
            }
            ownerType = getActualType(getRawClass(unactualized).getDeclaringClass(), actualizedOwner, assignments);
        }
        assignments.addFirst(new HashMap<TypeVariable<?>, Type>());
        getHierarchTypes(assignments.getFirst(), actualized);
        Type actual = unactualized;
        for (Map<TypeVariable<?>, Type> assignment : assignments) {
            actual = getActualType(actual, assignment);
        }
        if (actual instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) actual;
            return new Parameterized(pt.getRawType(), ownerType, pt.getActualTypeArguments());
        }
        return actual;
    }
  
    /**
     * Convert the given type into an integer type code to greatly simplify
     * equality testing by comparing codes, reducing the number of
     * <code>instanceof</code> tests. There is an integer value assigned to each
     * of the 5 classes derived from <code>Type</code>. The <code>Class</code>
     * and any types not derived from <code>Type</code> share the same code,
     * since classes are tested for equality using <code>Object.equals()</code>.
     * 
     * @param type
     *            The type.
     * @return An integer type.
     */
    private static int typeAsCode(Object type) {
        if (type instanceof GenericArrayType) {
            return 1;
        } 
        if (type instanceof ParameterizedType) {
            return 2;
        } 
        if (type instanceof WildcardType) {
            return 3;
        }
        if (type instanceof TypeVariable<?>) {
            return 4;
        } 
        return 5;
    }

    /**
     * Determine if the the two arrays of types are equal, comparing the type at
     * each element of one array against the type in the element at the same
     * index in the other array. The two arrays are equal if they are the same
     * length and if each element at each index is equal to the element in the
     * other array at the same index.
     * 
     * @param lefts
     *            An array of types to test for equality.
     * @param rights
     *            Another array of types to test for equality.
     * @return True if the arrays of types are equal.
     */
    private static boolean equals(Type[] lefts, Type[] rights) {
        if (lefts.length != rights.length) {
            return false;
        }
        for (int i = 0; i < lefts.length; i++) {
            if (!equals(lefts[i], rights[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if the two type objects are equal, recursively comparing their
     * properties if they are <code>Type</code> derived objects. This method
     * accepts <code>Object</code> and not <code>Type</code> so that is can be
     * used to compare the return value of
     * {@link TypeVariable#getGenericDeclaration()}, which is
     * <code>Object</code>.
     * <p>
     * Generally, both types are <code>Type</code> derived objects, they are
     * equals if they are the same derived type, and if all their members are
     * the same. The exception is <code>Class</code>, which is tested using
     * <code>Object.equals()</code>. <code>Class</code> an any other objects
     * that are not derived from <code>Object</code> are tested using
     * <code>Object.equals()</code>.
     * 
     * @param left
     *            An object to test for equality.
     * @param right
     *            Another object to test for equality.
     * @return True if the objects are equal.
     */
    public static boolean equals(Type left, Type right) {
        // Needed to test raw types of parameterized types.
        if (left == null || right == null) {
            return left == null && right == null;
        }
        int leftTypeAsCode = typeAsCode(left);
        if (leftTypeAsCode == typeAsCode(right)) {
            switch (leftTypeAsCode) {
            case 1:
                return equals(((GenericArrayType) left).getGenericComponentType(), ((GenericArrayType) right).getGenericComponentType());
            case 2:
                ParameterizedType ptLeft = (ParameterizedType) left;
                ParameterizedType ptRight = (ParameterizedType) right;
                return ptLeft.getRawType().equals(ptRight.getRawType())
                    && equals(ptLeft.getOwnerType(), ptRight.getOwnerType())
                    && equals(ptLeft.getActualTypeArguments(), ptRight.getActualTypeArguments());
            case 3:
                WildcardType wtLeft = (WildcardType) left;
                WildcardType wtRight = (WildcardType) right;
                return equals(wtLeft.getLowerBounds(), wtRight.getLowerBounds())
                    && equals(wtLeft.getUpperBounds(), wtRight.getUpperBounds());
            default:
                return left.equals(right);
            }
        }
        return false;
    }

    /**
     * Generate a hash code from the given list of <code>Type</code> instances.
     * <p>
     * The case of <code>A&lt;B extends C&lt;? super B&gt;&gt;</code> will
     * recurse, in equality too. A real problem for equality. Equality of type
     * variables hsa more to do with the position of the type varaible and its
     * definition, since it cannot change, and since we're not building nonsense
     * ones, are we? We're replacing them with actual types, not new type
     * variables, since this is an actualization library. So, type variables, we
     * cannot manufacture them, and they are defined in source, so we create a
     * hash from their declaration and their position in the parameter list, or
     * just their name, we can equate them using their declaring type and name.
     * 
     * @param types
     *            The types.
     * @return A hash code generated from the types.
     */
    public static int hashCode(Type...types) {
        int hashCode = 0;
        for (Type type : types) {
            if (type != null) {
                hashCode *= 37;
                switch (typeAsCode(type)) {
                case 1:
                    hashCode ^= hashCode(((GenericArrayType) type).getGenericComponentType());
                    break;
                case 2:
                    ParameterizedType pt = (ParameterizedType) type;
                    hashCode ^= pt.getRawType().hashCode() ^ hashCode(pt.getOwnerType()) ^ hashCode(pt.getActualTypeArguments());
                    break;
                case 3:
                    WildcardType wt = (WildcardType) type;
                    hashCode ^= hashCode(wt.getLowerBounds()) ^ hashCode(wt.getUpperBounds());
                    break;
                default:
                    hashCode ^=  type.hashCode();
                }
            }
        }
        return hashCode;
    }
}

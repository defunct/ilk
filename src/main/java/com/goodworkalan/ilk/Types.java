package com.goodworkalan.ilk;

import static java.util.Arrays.asList;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
        return (Class<?>) type;
    }

    static String typeToString(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getName();
        }
        return type.toString();
    }
    
    public static class Wildcard implements java.lang.reflect.WildcardType {
        private final Type[] lowerBounds;

        private final Type[] upperBounds;
        
        public Wildcard(Type[] lowerBounds, Type[] upperBounds) {
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
        }
        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }
        
        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }  

        public boolean equals(Object object) {
            if (object instanceof Types.Wildcard) {
                Types.Wildcard wt = (Types.Wildcard) object;
                return Arrays.equals(lowerBounds, wt.lowerBounds)
                    && Arrays.equals(upperBounds, wt.upperBounds);
            }
            return false;
        }
        
        public int hashCode() {
            return Arrays.hashCode(lowerBounds) * 37 + Arrays.hashCode(lowerBounds);
        }

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
         * @param pt
         *            The parameterized type.
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
        
        public boolean equals(Object object) {
            if (object instanceof Types.Parameterized) {
                Types.Parameterized pt = (Types.Parameterized) object;
                List<Object> to = asList(rawType, ownerType, asList(actualTypeArguments));
                List<Object> from = asList(pt.getRawType(), pt.getOwnerType(), asList(pt.getActualTypeArguments()));
                return to.equals(from);
            }
            return false;
        }
        
        public int hashCode() {
            return asList(rawType, ownerType, Arrays.hashCode(actualTypeArguments)).hashCode();
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
                return asList(((ParameterizedType) to).getActualTypeArguments()).equals(asList(((ParameterizedType) from).getActualTypeArguments()));
            }
            return true;
        }
        return false;
    }

    public static boolean isEquitable(Type type) {
        return !((type instanceof ParameterizedType) || (type instanceof WildcardType)) || Types.class.equals(type.getClass().getDeclaringClass());
    }

    public static Type getEquatable(Type type) {
        if (isEquitable(type)) {
            return type;
        }
        return getActualType(type, type);
    }

    public static void getHierarchTypes(Map<TypeVariable<?>, Type> map, Type source) {
        if (source != null) {
            if (source instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) source;
                Type[] arguments = pt.getActualTypeArguments();
                TypeVariable<?>[] parameters = getRawClass(source).getTypeParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Type assignment = map.get(arguments[i]);
                    map.put(parameters[i], assignment == null ? arguments[i] : assignment);
                }
            }
            for (Type iface : getRawClass(source).getGenericInterfaces()) {
                getHierarchTypes(map, iface);
            }
            getHierarchTypes(map, getRawClass(source).getGenericSuperclass());
        }
    }

    public static boolean isAssignableFrom(Type to, Type from) {
        if (getRawClass(to).isAssignableFrom(getRawClass(from))) {
            if (to instanceof Class<?>) { 
                return true;
            }
            Type actualFrom = getActualType(getRawClass(to), from);
            Types.Parameterized pt = (Types.Parameterized) getEquatable(to);
            Type[] typesTo = pt.getActualTypeArguments();
            Type[] typesFrom = ((ParameterizedType) actualFrom).getActualTypeArguments();
            for (int i = 0; i < typesTo.length; i++) {
                if (typesTo[i] instanceof WildcardType && !(typesFrom[i] instanceof WildcardType)) {
                    if (!checkWildcardType((WildcardType) typesTo[i], typesFrom[i], false)) {
                        return false;
                    }
                } else if (!typesTo[i].equals(typesFrom[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

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
     * <p>
     * This is going to be hard to remember...
     * <p>
     * Key to understanding is first that the <code>unactualized</code> must be
     * a super-type of <code>actualized<code>
     * found using the {@link #getSuperType(Type, Class)} method, and second
     * that the type variables can be empty, they are for assigning types to 
     * type variables that are external to the <code>actualized</code> type
     * hierarchy. If a type is created with a method type variable, say, it
     * becomes a part of the <code>actualized</code> type. The
     * <code>assignments</code> map can be used to assign those
     * <code>TypeVariable</code> occurrences.
     * <p>
     * Once you've called this method, you'll have a parameterized type map...
     * <p>
     * Because this is in support of Ilk, when you call this method with a
     * non-parameterized type and an empty type map, this method is a no-op and
     * returns the given type. This method is used to initialize an
     * <code>Ilk.Key</code> and create a type that is comparable and assignable.
     * This way I can keep from bulking up the library with conditionals and
     * flavors of this method. I was really counting bytes and didn't want to
     * check the type used to construct an <code>Ilk.Key</code>, when I could
     * code this method to do nothing and return the type.
     * <p>
     * Funny thing, I can see how this works now after I've written it, and I
     * spent a day not understanding the magic. It was built through testing, so
     * as the tests passed, I knew it was correct (simply, it was making the
     * same assertions as the Java compiler), but I didn't get this bit until
     * just now...
     * <p>
     * It is two methods, depending on the parameters passed. When unactualized
     * is the same as actualized, then the method will iterate of over the type
     * declaration and assign any type variables from the
     * <code>assignments</code> map.
     * <p>
     * I guess I didn't want to write the iteration twice, and it almost worked
     * too. It terminates the search when it hits a type variable and replaces
     * it with a resolved variable. When there is an actualized type on an
     * unactualized type, this stops for each of the unactualized type
     * parameters. When it is an assignment, all of the parameters are
     * actualized except for the missing external parameters, but that is if you
     * only have one level, well it was, I changed it so that if there are
     * parameters will will navigate the parameterized type, not the type
     * arguments. It is really tricky code, so you're going to be really
     * confused when you read it again. I don't envy you.
     * <p>
     * The given <code>type</code> is an actualized sub-type of the given
     * <code>actualized</code> type. It is found using the
     * {@link #getSuperType(Type, Class) getSuperType} method.
     * 
     * @param unactualized
     * @param actualized
     * @param assignments
     *            The map of type variables to actual variables to map types
     *            that are actualized in the type hierarchy.
     * @return
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
    
    public static boolean _hasRawClass(Type type) {
        return (type instanceof Class<?>) || (type instanceof ParameterizedType);
    }

    public static Type getActualType(Type unactualized, Type actualized, LinkedList<Map<TypeVariable<?>, Type>> assignments) {
        Type ownerType = null;
        if (getRawClass(unactualized).getDeclaringClass() != null) {
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

    public static Type getActualType(Type unactualized, Type actualized) {
        boolean isEquitable = Types.class.equals(unactualized.getClass().getDeclaringClass());
        if (isEquitable) {
            throw new IllegalArgumentException(); 
        }
        if ((unactualized instanceof Class<?>) || (unactualized instanceof ParameterizedType)) {
            return getActualType(getRawClass(unactualized), actualized, new LinkedList<Map<TypeVariable<?>, Type>>());
        }
        return getActualType(unactualized, Collections.<TypeVariable<?>, Type>emptyMap());
    }
    
    private static int typeAsCode(Object type) {
        if (type instanceof GenericArrayType) {
            return 1;
        } 
        if (type instanceof ParameterizedType) {
            return 2;
        } 
        if (type instanceof TypeVariable<?>) {
            return 3;
        } 
        if (type instanceof WildcardType) {
            return 4;
        }
        return 5;
    }

    public static boolean equals(Type[] lefts, Type[] rights) {
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

    public static boolean equals(Object left, Object right) {
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
                TypeVariable<?> tvLeft = (TypeVariable<?>) left;
                TypeVariable<?> tvRight = (TypeVariable<?>) right;
                return tvLeft.getName().equals(tvRight.getName())
                    && equals(tvLeft.getGenericDeclaration(), tvRight.getGenericDeclaration())
                    && equals(tvLeft.getBounds(), tvRight.getBounds());
            case 4:
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
    
    public static int hashCode(Type...types) {
        int hashCode = 1;
        for (Type type : types) {
            hashCode *= 37;
            if (type == null) {
                hashCode = hashCode * 37 + 1;
            } else if (type instanceof WildcardType) {
                WildcardType wt = (WildcardType) type;
                hashCode ^= hashCode(wt.getLowerBounds()) ^ hashCode(wt.getUpperBounds());
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                hashCode ^= pt.getRawType().hashCode() ^ hashCode(pt.getOwnerType()) ^ hashCode(pt.getActualTypeArguments());
            } else if (type instanceof TypeVariable<?>) {
                TypeVariable<?> tv = (TypeVariable<?>) type;
                hashCode ^= hashCode(tv.getBounds()) ^ tv.getName().hashCode();
                hashCode ^= (tv.getGenericDeclaration() instanceof Member) ? tv.getGenericDeclaration().hashCode() : hashCode((Type) tv.getGenericDeclaration());
            } else if (type instanceof GenericArrayType) {
                hashCode ^= hashCode(((GenericArrayType) type).getGenericComponentType());
            } else {
                hashCode ^= ((Class<?>) type).hashCode();
            }
        }
        return hashCode;
    }
}

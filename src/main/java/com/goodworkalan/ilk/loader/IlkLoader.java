package com.goodworkalan.ilk.loader;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Types;

/**
 * Loads types creating new instances of boxed super type tokens by parsing type
 * definition strings.
 * 
 * @author Alan Gutierrez
 */
public class IlkLoader {
    /** A Java identifier. */
    private final static String IDENTIFIER = "[$_\\w&&[^\\d]][$\\w]+";

    /**
     * Match a class name and the next possible character of after the class
     * name, either a comma, or opening or closing type parameter list bracket.
     */
    private final static Pattern TYPE_DEFINITION = Pattern.compile("((?:(?:" +IDENTIFIER + "\\s*\\.?\\s*)+)?)([,><]?)");

    /**
     * Parse the give type name and load the classes defined by the type name
     * using the given class loader. The map of imports maps class names to
     * fully qualified class names, so that the type name can use just class
     * names to load the types and client programs can define an import
     * facility.
     * 
     * @param <T>
     *            The working type variable.
     * @param classLoader
     *            The class loader.
     * @param typeDefinition
     *            The type definition to parse.
     * @param imports
     *            The map of class names to fully qualified class names.
     * @return The boxed super type token for the type name.
     * @throws ClassNotFoundException
     *             If any of the classes in the type definition cannot be found.
     */
    public static <T> Ilk.Box fromString(ClassLoader classLoader, String typeDefinition, Map<String, Class<?>> imports)
    throws ClassNotFoundException {
        return load(classLoader, typeDefinition, imports);
    }

    /**
     * Parse the give type name and load the classes defined by the type name
     * using the given class loader. The map of imports maps class names to
     * fully qualified class names, so that the type name can use just class
     * names to load the types and client programs can define an import
     * facility.
     * <p>
     * This method is internal to hide the working type parameter, which would
     * require to much explanation in the public API documentation.
     * 
     * @param <T>
     *            The working type variable.
     * @param classLoader
     *            The class loader.
     * @param typeDefinition
     *            The type definition to parse.
     * @param imports
     *            The map of class names to fully qualified class names.
     * @return The boxed super type token for the type name.
     * @throws ClassNotFoundException
     *             If any of the classes in the type definition cannot be found.
     */
     private static <T> Ilk.Box load(ClassLoader classLoader, String typeDefinition, Map<String, Class<?>> imports)
     throws ClassNotFoundException {
        List<Type> arguments = new ArrayList<Type>();
        getTypeArguments(arguments, classLoader, typeDefinition.trim(), imports);
        if (arguments.size() != 1) {
            throw new IllegalArgumentException();
        }
        return new Ilk<T>() {}.assign(new Ilk<T>() {}, arguments.get(0)).box();
    }

    /**
     * Parse the given type definition recursively, calling this method once
     * again for any type parameters in the given type definition. The method
     * ignores the rest of the string beyond the type class name or type
     * parameter closing bracket, so that the method can be called recursively,
     * stopping when it reaches the end of the nested type definition and
     * returning the remaining bit of string.
     * 
     * @param arguments
     *            The list of type arguments.
     * @param classLoader
     *            The class loader.
     * @param typeDefinition
     *            The the type definition to parse.
     * @param imports
     *            The map of class names to fully qualified class names.
     * @return The remaining unparsed type definition string.
     * @throws ClassNotFoundException
     *             If any of the classes in the type definition cannot be found.
     */
     private static String getTypeArguments(List<Type> arguments, ClassLoader classLoader, String typeDefintion, Map<String, Class<?>> imports) throws ClassNotFoundException {
        Matcher matcher = TYPE_DEFINITION.matcher(typeDefintion);
        if (matcher.lookingAt()) {
            String className = matcher.group(1).replaceAll("\\s+", "");
            Class<?> type = imports.get(className);
            if (type == null) {
                type = classLoader.loadClass(className);
            }
            if (matcher.group(2).equals("<")) {
                List<Type> subArguments = new ArrayList<Type>();
                String remaining = typeDefintion.substring(matcher.end() - 1);
                while ((remaining = getTypeArguments(subArguments, classLoader, remaining.substring(1).trim(), imports)).startsWith(",")) {
                }
                arguments.add(new Types.Parameterized(type, type.getDeclaringClass(), subArguments.toArray(new Type[subArguments.size()])));
                return remaining.substring(1).trim();
            } 
            if (matcher.group(2).equals(">")) {
                arguments.add(type);
                return typeDefintion.substring(matcher.end() - 1).trim();
            } 
            if (matcher.group(2).equals(",")){
                arguments.add(type);
                return getTypeArguments(arguments, classLoader, typeDefintion.substring(matcher.end()).trim(), imports);
            }
            arguments.add(type);
            return typeDefintion.substring(matcher.end()).trim();
        }
        return "";
    }
}

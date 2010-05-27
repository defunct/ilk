package com.goodworkalan.ilk.loader;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Types;

public class IlkLoader {
    public static <T> Ilk.Box fromString(ClassLoader classLoader, String typeName, Map<String, Class<?>> imports)
    throws ClassNotFoundException {
        List<Type> arguments = new ArrayList<Type>();
        getTypeArguments(arguments, classLoader, typeName.trim(), imports);
        if (arguments.size() != 1) {
            throw new IllegalArgumentException();
        }
        return new Ilk<T>() {}.assign(new Ilk<T>() {}, arguments.get(0)).box();
    }
    
    private final static String identifier = "[$_\\w&&[^\\d]][$\\w]+";

    private final static Pattern a = Pattern.compile("((?:(?:" +identifier + "\\s*\\.?\\s*)+)?)([,><]?)");

    private static String getTypeArguments(List<Type> arguments, ClassLoader classLoader, String typeNames, Map<String, Class<?>> imports) throws ClassNotFoundException {
        Matcher matcher = a.matcher(typeNames);
        if (matcher.lookingAt()) {
            String className = matcher.group(1).replaceAll("\\s+", "");
            Class<?> type = imports.get(className);
            if (type == null) {
                type = classLoader.loadClass(className);
            }
            if (matcher.group(2).equals("<")) {
                List<Type> subArguments = new ArrayList<Type>();
                String remaining = typeNames.substring(matcher.end() - 1);
                while ((remaining = getTypeArguments(subArguments, classLoader, remaining.substring(1).trim(), imports)).startsWith(",")) {
                }
                arguments.add(new Types.Parameterized(type, type.getDeclaringClass(), subArguments.toArray(new Type[subArguments.size()])));
                return remaining.substring(1).trim();
            } 
            if (matcher.group(2).equals(">")) {
                arguments.add(type);
                return typeNames.substring(matcher.end() - 1).trim();
            } 
            if (matcher.group(2).equals(",")){
                arguments.add(type);
                return getTypeArguments(arguments, classLoader, typeNames.substring(matcher.end()).trim(), imports);
            }
            arguments.add(type);
            return typeNames.substring(matcher.end()).trim();
        }
        return "";
    }
}

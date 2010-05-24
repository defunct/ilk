package com.goodworkalan.ilk.association;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Types;
import com.goodworkalan.ilk.Ilk.Key;

import static com.goodworkalan.ilk.Types.getRawClass;

/**
 * Associates a value to a type by either matching the a given super type token
 * exactly, mapping an annotation applied to the class, or by matching any super
 * type token whose type is assignable to the type in a given super type token.
 * These three different match conditions are specified independently, so that
 * you can chose to associate by class, by annotation, or by class and its
 * descendants.
 * <p>
 * You specify an association by an exact match using the
 * {@link #exact(Key, Object) exact} method. A match by an annotation applied to
 * a class is specified using the {@link #annotated(Class, Object) annotated}
 * method. A match specified by matching any class that is assignable to a given
 * class is specified by the {@link #assignable(Key, Object) derived} method.
 * <p>
 * An exact match will take precedence over an annotation or assignment match.
 * An annotation match will take precedence over an assignment match.
 * <p>
 * This class is thread safe. All associations are stored in concurrent maps so
 * the behavior associated with concurrent maps, where a value written in one
 * thread is not yet be readable in another thread until the write is complete,
 * applies to this class.
 * <p>
 * After an association lookup, the result is cached, so that reflection and
 * class hierarchy navigation does not have to be repeated. Assigning new
 * associations resets the cache.
 * <p>
 * The concurrency model is designed to support the use of the
 * <code>IlkAssociation</code> class as a static object. If there is a static
 * association of helper objects by type, a new class may be loaded by the class
 * loader, initialize itself in its static initialization block by registering a
 * helper object for its type, and the helper object would be ready for lookup
 * by the time an instance of newly loaded object needed its helper object.
 * 
 * @author Alan Gutierrez
 * 
 * @param <T>
 *            The type of value associated.
 */
public class IlkAssociation<T> {
    /** Whether or not a new mapping value replaces an old mapping value. */
    private final boolean multi;
    
    /**
     * The classes to their object diffusers as resolved by ascending the object
     * hierarchy, looking for an object diffuser that will diffuse a super class
     * or interface. This cache is reset when a new object diffuser is assigned
     * using the {@link #setConverter(Class, ObjectDiffuser) setConverter}
     * method.
     */
    private final Map<Ilk.Key, Queue<T>> cache = new ConcurrentHashMap<Ilk.Key, Queue<T>>();

    /** Map of assignable classes to values. */
    private final Map<Class<?>, Map<Ilk.Key, List<T>>> assignable = new ConcurrentHashMap<Class<?>, Map<Ilk.Key, List<T>>>();

    /** Map of exact classes to values. */
    private final Map<Class<?>, Map<Ilk.Key, List<T>>> exact = new ConcurrentHashMap<Class<?>, Map<Ilk.Key, List<T>>>();
    
    /** Map of class annotations to values. */
    private final Map<Class<? extends Annotation>, List<T>> annotated = new ConcurrentHashMap<Class<? extends Annotation>, List<T>>();

    /**
     * Create an empty class association that stores multiple values for a
     * mapping if the value given for <code>multi</code> is true.
     * 
     * @param multi
     *            If true, multiple values are associated with each mapping, if
     *            false an new mapping value replaces the old mapping value.
     */
    public IlkAssociation(boolean multi) {
        this.multi = multi;
    }

    /**
     * Create an ilk association that is a copy of the given ilk association.
     * All of the internal state is copied, so that changes made to the copied
     * class association do not affect the new class association.
     * 
     * @param copy
     *            The class association to copy.
     */
    public IlkAssociation(IlkAssociation<T> copy) {
        this.multi = copy.multi;
        addAll(copy);
    }
    
    public synchronized void addAll(IlkAssociation<T> other) {
        addAll(exact, other.exact);
        addAll(assignable, other.assignable);
        for (Map.Entry<Class<? extends Annotation>, List<T>> entry : other.annotated.entrySet()) {
            for (T value : entry.getValue()) {
                annotated(entry.getKey(), value);
            }
        }
    }
    
    private void addAll(Map<Class<?>, Map<Ilk.Key, List<T>>> dest, Map<Class<?>, Map<Ilk.Key, List<T>>> source) {
        for (Map<Ilk.Key, List<T>> ilks : source.values()) {
            for (Ilk.Key key : ilks.keySet()) {
                dest.put(getRawClass(key.type), add(dest, key, ilks.get(key)));
            }
        }
    }

    /**
     * Map the given<code>value</code> to the given ilk <code>key</code>
     * exactly. Sub-classes or implementations of the given type will not be
     * associated with the given <code>value</code> as a result of this
     * association.
     * <p>
     * This method is thread safe since all mappings are kept in concurrent
     * maps.
     * <p>
     * Calling this method will reset the internal cache of resolved mappings.
     * <p>
     * While this method is synchronized, the get methods are not. This method
     * is synchronized to prevent read write conflicts of the list of values
     * that is mapped to each raw type. The list is itself unsynchronized, so
     * new items are added via a copy. The synchronization prevents the
     * read-write conflict where two threads create a copy, then one thread
     * after another adds an item to their isolated copy and returns the copy to
     * the items by class map. Because the map is a concurrent map and because
     * the lists are never changed after they have been added to the map, map
     * lookup does not need to be synchronized.
     * 
     * @param type
     *            The type.
     * @param value
     *            The value to associate with the type.
     */
    public synchronized void exact(Ilk.Key key, T value) {
        Map<Ilk.Key, List<T>> pairs = add(exact, key, Collections.singletonList(value));
        exact.put(getRawClass(key.type), pairs);
        cache.clear();
    }

    /**
     * Create a deep copy of the ilk pair list found in the given map using the
     * raw class of the given key and then add the given value to the list of
     * values paired with the given key. If this is not a multi association, the
     * copied list is cleared before the value is added.
     * 
     * @param map
     *            The map.
     * @param key
     *            The key.
     * @param value
     *            The value.
     * @return A copy of the ilk pair list with the value added to the list
     *         associated with the key.
     */
    private Map<Ilk.Key, List<T>> add(Map<Class<?>, Map<Ilk.Key, List<T>>> map, Ilk.Key key, List<T> values) {
        Map<Ilk.Key, List<T>> pairs = copy(getRawClass(key.type), map);
        for (Map.Entry<Ilk.Key, List<T>> pair : pairs.entrySet()) {
            if (pair.getKey().equals(key)) {
                if (multi) {
                    pair.getValue().addAll(values);
                } else {
                    pair.getValue().clear();
                    pair.getValue().add(values.get(0));
                }
                return pairs;
            }
        }
        pairs.put(key, values);
        return pairs;
    }

    /**
     * Create a copy of ilk to queue pair list in the given map for the given
     * raw class, or create a new list if the key does not exist.
     * 
     * @param map
     *            The map.
     * @param rawClass
     *            The key.
     * @return A queue that contains the elements currently associated with key
     *         that need to be preserved when a new value is associated with the
     *         key.
     */
    private Map<Ilk.Key, List<T>> copy(Class<?> rawClass, Map<Class<?>, Map<Ilk.Key, List<T>>> map) {
        Map<Ilk.Key, List<T>> copy = new HashMap<Ilk.Key, List<T>>();
        Map<Ilk.Key, List<T>> pairs = map.get(rawClass);
        if (pairs != null) {
            for (Map.Entry<Ilk.Key, List<T>> pair : pairs.entrySet()) {
                copy.put(pair.getKey(), new ArrayList<T>(pair.getValue()));
            }
        }
        return copy;
    }

    /**
     * Map the given <code>value</code> to the type represented by the given ilk
     * <code>key</code>, sub-classes of the given <code>type</code> or
     * implementations of the given <code>type</code> . The value will match an
     * assignable association if there is no exact association or annotation
     * association that matches the given <code>type</code> in this class
     * association.
     * <p>
     * If a type can match multiple super-classes or interfaces, it will match
     * the first mapping or interface association encountered when inspecting
     * the class and super classes of given type. The class is first tested,
     * then each super-class of the class it tested, from the class itself up
     * the inheritance hierarchy to <code>Object</code>. For each class in the
     * hierarchy, all of the interfaces implemented by the class and all of the
     * interfaces that those interfaces extend are checked for a derived
     * association. The first derived association to match is returned.
     * <p>
     * If a class directly implements or an interface directly extends two or
     * more interfaces that have a derived association, there is no telling
     * which of the two or more associations will be chosen. Otherwise, you can
     * determine which derived association will be chosen by working your way up
     * the class and interface hierarchy according to the rules.
     * 
     * @param key
     *            The super type token.
     * @param converter
     *            The value to associate with the type.
     */
    public synchronized void assignable(Ilk.Key key, T value) {
        Map<Ilk.Key, List<T>> pairs = add(assignable, key, Collections.singletonList(value));
        assignable.put(getRawClass(key.type), pairs);
        cache.clear();
    }

    /**
     * Map the given <code>value</code> to classes annotated with the given
     * <code>annotation</code> type. The value will match a annotated
     * association if there is no exact association that matches the given type.
     * If a type is annotated by two ore more annotations that have associated
     * values, their is no telling which of the values will be returned.
     * <p>
     * Annotations applied to super-classes or interfaces implemented by the
     * type will not be considered when determining the match.
     * 
     * @param type
     *            The annotation type.
     * @param converter
     *            The value to associate with the annotation type.
     */
    public synchronized void annotated(Class<? extends Annotation> annotation, T value) {
        List<T> values = annotated.get(annotation);
        if (values != null && multi) {
            values = new ArrayList<T>(values);
            values.add(value);
        } else {
            values = Collections.singletonList(value);
        }
        annotated.put(annotation, values);
        cache.clear();
    }


    /**
     * Get the object converter for the given object type.
     * 
     * @param type
     *            The object type.
     * @return The object converter.
     */
    public T get(Ilk.Key key) {
        return getQueue(key).peek();
    }
    
    public List<T> getAll(Ilk.Key key) {
        return new ArrayList<T>(getQueue(key));
    }
    
    private Queue<T> getQueue(Ilk.Key key) {
        Queue<T> values = cache.get(key);
        if (values == null) {
            values = new LinkedList<T>();
//            if (type.isArray()) {
//                throw new IllegalArgumentException();
//            }
//            if (type.isPrimitive()) {
//                throw new IllegalArgumentException();
//            }
            Map<Ilk.Key, List<T>> pairs = exact.get(getRawClass(key.type));
            if (pairs != null) {
                for (Map.Entry<Ilk.Key, List<T>> pair : pairs.entrySet()) {
                    if (pair.getKey().equals(key)) {
                        values.addAll(pair.getValue());
                        break;
                    }
                }
            }
            for (Annotation annotation : getRawClass(key.type).getAnnotations()) {
                // Get to shrink the code a few bytes when I use annotatedType().
                for (Map.Entry<Class<? extends Annotation>, List<T>> entry : annotated.entrySet()) {
                    if (entry.getKey().isAssignableFrom(annotation.getClass())) {
                        values.addAll(entry.getValue());
                    }
                }
            }
            Set<Type> seen = new HashSet<Type>();
            Type iterator = key.type;
            while (iterator != null) {
                LinkedList<Type> interfaces = new LinkedList<Type>();
                interfaces.add(iterator);
                while (!interfaces.isEmpty()) {
                    Type iface = interfaces.removeFirst();
                    if (!seen.contains(getRawClass(iface))) {
                        seen.add(getRawClass(iface));
                        pairs = assignable.get(getRawClass(iface));
                        if (pairs != null) {
                            for (Map.Entry<Ilk.Key, List<T>> pair : pairs.entrySet()) {
                                TreeMap<Ilk.Key, List<T>> sorted = new TreeMap<Key, List<T>>(new Comparator<Ilk.Key>() {
                                    public int compare(Key o1, Key o2) {
                                        if (o1.isAssignableFrom(o2)) {
                                            return -1;
                                        }
                                        if (o2.isAssignableFrom(o1)) {
                                            return 1;
                                        }
                                        throw new IllegalStateException();
                                    }
                                });
                                if (pair.getKey().isAssignableFrom(new Ilk.Key(Types.getActualType(getRawClass(iface), key.type)))) {
                                    sorted.put(pair.getKey(), pair.getValue());
                                }
                                for (List<T> found : sorted.values()) {
                                    values.addAll(found);
                                }
                            }
                        }
                        for (Type ifaceIFace : getRawClass(iface).getInterfaces()) {
                            interfaces.add(ifaceIFace);
                        }
                    }
                }
                iterator = getRawClass(iterator).getSuperclass();
            }

            cache.put(key, values);
        }
        return values;
    }

    /**
     * Often times the contents have typed values, ilk created, so it's best to
     * poke it in through the front, since that's where the compiler type
     * information will be. Tried having a missing method, but there was no type
     * information with ilk.
     * 
     * @param key
     *            The key.
     * @param values
     *            The cache values.
     */
    public void cache(Ilk.Key key, List<T> values) {
        cache.put(key, new LinkedList<T>(values));
    }
}
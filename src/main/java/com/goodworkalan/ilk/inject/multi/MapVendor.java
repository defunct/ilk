package com.goodworkalan.ilk.inject.multi;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.Vendor;

/**
 * A vendor that provides a map of object values provided by the injector.
 * 
 * @author ALan Gutierrez
 * 
 * @param <K>
 *            The key type.
 * @param <V>
 *            The injected value type.
 */
public class MapVendor<K, V> extends Vendor<Map<K, V>>  {
    /** The map of keys to vendors that provided the value instances. */
    private final Map<K, Vendor<? extends V>> vendors;

    /**
     * Create a map vendor that will create a map by invoking the instance
     * method of each of the vendors in the map list of vendors, and assigning
     * the output to a new map of instances, using the vendor's map key.
     * 
     * @param vendors
     *            The map of vendors.
     * @param keyIlk
     *            The super type token of the vendor.
     * @param valueIlk
     *            The super type token of type of the value to vend.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope in which to store the constructed map.
     */
    public MapVendor(Map<K, Vendor<? extends V>> vendors, Ilk<K> keyIlk, Ilk<V> valueIlk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(new Ilk<Map<K, V>>(){}.assign(new Ilk<Ilk<K>>() {}, keyIlk).assign(new Ilk<Ilk<V>>(){}, valueIlk), qualifier, scope, IlkReflect.REFLECTOR);
        this.vendors = vendors;
    }

    /**
     * Vend an instance of the map.
     * 
     * @param injector
     *            The injector.
     * @return A boxed instance of the map.
     */
    public Ilk.Box get(Injector injector) {
        Map<K, V> map = new LinkedHashMap<K, V>();
        for (Map.Entry<K, Vendor<? extends V>> entry : vendors.entrySet()) {
            map.put(entry.getKey(), injector.instance(entry.getValue()));
        }
        return ilk.box(map);
    }
}

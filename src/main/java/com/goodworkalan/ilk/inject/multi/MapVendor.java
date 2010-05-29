package com.goodworkalan.ilk.inject.multi;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.Vendor;

class MapVendor<K, V> extends Vendor<Map<K, V>>  {
    private final Map<K, Vendor<V>> vendors;
    
    public MapVendor(Map<K, Vendor<V>> builders, Ilk<K> keyIlk, Ilk<V> valueIlk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(new Ilk<Map<K, V>>(){}.assign(new Ilk<Ilk<K>>() {}, keyIlk).assign(new Ilk<Ilk<V>>(){}, valueIlk), qualifier, scope, IlkReflect.REFLECTOR);
        this.vendors = builders;
    }

    public Ilk.Box get(Injector injector) {
        Map<K, V> map = new LinkedHashMap<K, V>();
        for (Map.Entry<K, Vendor<V>> entry : vendors.entrySet()) {
            map.put(entry.getKey(), injector.instance(entry.getValue()));
        }
        return ilk.box(map);
    }
}

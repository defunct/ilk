package com.goodworkalan.ilk.inject;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;

class MapVendor<K, V> extends VendorProviderVendor {
    private final Map<K, Vendor> builders;
    
    private final Ilk<K> keyIlk;

    private final Ilk<V> valueIlk;
    
    public MapVendor(Map<K, Vendor> builders, Ilk<K> keyIlk, Ilk<V> valueIlk) {
        super(new Ilk<Provider<Map<K, V>>>(keyIlk.key, valueIlk.key) { }.key);
        this.builders = builders;
        this.keyIlk = keyIlk;
        this.valueIlk = valueIlk;
    }

    public Box instance(Injector injector) {
        Map<K, V> map = new LinkedHashMap<K, V>();
        for (Map.Entry<K, Vendor> entry : builders.entrySet()) {
            map.put(entry.getKey(), entry.getValue().instance(injector).cast(valueIlk));
        }
        return new Ilk<Map<K, V>>(keyIlk.key, valueIlk.key) {}.box(map);
    }
}

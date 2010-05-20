package com.goodworkalan.ilk.inject;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;

public class MapBuilder<K, V> extends MissingProviderBuilder {
    private final Map<K, Builder> builders;
    
    private final Ilk<K> keyIlk;

    private final Ilk<V> valueIlk;
    
    public MapBuilder(Map<K, Builder> builders, Ilk<K> keyIlk, Ilk<V> valueIlk) {
        super(new Ilk<Provider<Map<K, V>>>(keyIlk.key, valueIlk.key) { }.key, map(keyIlk, valueIlk).key);
        this.builders = builders;
        this.keyIlk = keyIlk;
        this.valueIlk = valueIlk;
    }
    
    static <K, V> Ilk<Map<K, V>> map(Ilk<K> keyIlk, Ilk<V> valueIlk) {
        return new Ilk<Map<K, V>>(keyIlk.key, valueIlk.key) {};
    }

    public Box instance(Injector injector) {
        Map<K, V> map = new LinkedHashMap<K, V>();
        for (Map.Entry<K, Builder> entry : builders.entrySet()) {
            map.put(entry.getKey(), entry.getValue().instance(injector).cast(valueIlk));
        }
        return map(keyIlk, valueIlk).box(map);
    }
}

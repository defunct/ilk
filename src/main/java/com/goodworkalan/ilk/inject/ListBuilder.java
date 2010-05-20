package com.goodworkalan.ilk.inject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;

public class ListBuilder<T> extends MissingProviderBuilder {
    private final List<Builder> builders;
    
    private final Ilk<T> type;
    
    public ListBuilder(List<Builder> builders, Ilk<T> type) {
        super(new Ilk<Provider<List<T>>>(type.key) {}.key, list(type).key);
        this.builders = builders;
        this.type = type;
    }
    
    static <I> Ilk<List<I>> list(Ilk<I> ilk) {
        return new Ilk<List<I>>(ilk.key) {};
    }

    public Box instance(Injector injector) {
        List<T> built = new ArrayList<T>();
        for (Builder builder : builders) {
            built.add(builder.instance(injector).cast(type));
        }
        return list(type).box(built);
    }
}

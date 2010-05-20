package com.goodworkalan.ilk.inject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;

class ListVendor<I> extends VendorProviderVendor {
    private final List<Vendor> builders;
    
    private final Ilk<I> type;
    
    public ListVendor(List<Vendor> builders, Ilk<I> type) {
        super(new Ilk<Provider<List<I>>>(type.key) {}.key);
        this.builders = builders;
        this.type = type;
    }

    public Box instance(Injector injector) {
        List<I> built = new ArrayList<I>();
        for (Vendor builder : builders) {
            built.add(builder.instance(injector).cast(type));
        }
        return new Ilk<List<I>>(type.key) {}.box(built);
    }
}

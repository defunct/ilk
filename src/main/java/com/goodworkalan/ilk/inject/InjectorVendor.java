package com.goodworkalan.ilk.inject;

import com.goodworkalan.ilk.Ilk;

class InjectorVendor extends Vendor<Injector> {
    public InjectorVendor() {
        super (new Ilk<Injector>(Injector.class), NoQualifier.class, NoScope.class);
    }

    protected Ilk.Box get(Injector injector) {
        return new Ilk<Injector>(Injector.class).box(injector);
    }
}

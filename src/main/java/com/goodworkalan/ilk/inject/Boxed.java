package com.goodworkalan.ilk.inject;

import com.goodworkalan.ilk.Ilk;

public class Boxed<T> {
    public final Ilk.Box box;
    
    public Boxed(Ilk.Box box) {
        this.box = box;
    }
}

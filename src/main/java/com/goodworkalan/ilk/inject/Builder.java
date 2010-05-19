package com.goodworkalan.ilk.inject;

import java.util.LinkedList;

import com.goodworkalan.ilk.Ilk;

interface Builder {
    public Ilk.Box instance(LinkedList<QualifiedType> stack, Injector injector);        

    public Ilk.Box provider(LinkedList<QualifiedType> stack, Injector injector);        
}

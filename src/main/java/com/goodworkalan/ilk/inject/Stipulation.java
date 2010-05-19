package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;


class Stipulation {
    public final Builder builder;
    
    public final Class<? extends Annotation> scope;
    
    public Stipulation(Builder builder, Class<? extends Annotation> scope) {
        this.builder = builder;
        this.scope = scope;
    }
}

package com.goodworkalan.ilk.api;

import java.util.List;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.Ilk;

public class PublicTest {
    @Test
    public void cast() {
        foo(new Ilk<List<String>>() {});
    }
    
    public static <T> void foo(Ilk<T> ilk) {
        System.out.println(ilk);
        System.out.println(bar(ilk));
    }
    
    public static <T> Ilk<List<T>> bar(Ilk<T> ilk) {
        return new Ilk<List<T>>() { }.assign(new Ilk<Ilk<T>>(){}, ilk);
    }
}

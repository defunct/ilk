package com.goodworkalan.ilk.api;

import java.util.List;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Types;

/**
 * Tests of the public API.
 *
 * @author Alan Gutierrez
 */
public class PublicTest {
    /** Test assign with method type variables. */
    @Test
    public void cast() {
        makeTypes(new Ilk<List<String>>() {});
    }
    
    /**
     * Test assign with method type variables.
     * 
     * @param ilk
     *            A super type token from which a new token will be derived.
     */
    public static <T> void makeTypes(Ilk<T> ilk) {
        System.out.println(ilk);
        System.out.println(asList(ilk));
    }

    /**
     * Create a list super type token that contains the type of the given super
     * type token.
     * 
     * @param ilk
     *            A super type token from which a new token will be derived.
     * @return A super type token for a list that contains the given super type
     *         token type.
     */
    public static <T> Ilk<List<T>> asList(Ilk<T> ilk) {
        return new Ilk<List<T>>() { }.assign(Types.getMethods(PublicTest.class, "asList")[0].getTypeParameters()[0], ilk.key.type);
    }
}

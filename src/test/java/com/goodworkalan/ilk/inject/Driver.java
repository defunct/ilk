package com.goodworkalan.ilk.inject;

import javax.inject.Inject;

/**
 * A driver.
 * 
 * @author Alan Gutierrez
 */
public class Driver {
    /** The sunglasses. */
    @Inject
    public Sunglasses sunglasses;
    
    /** The license. */
    public final License license;
    
    /**
     * The license.
     * 
     * @param license The license.
     */
    public Driver(License license) {
        this.license = license;
    }
}

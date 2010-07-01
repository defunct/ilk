package com.goodworkalan.ilk.inject;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provide a driver.
 *
 * @author Alan Gutierrez
 */
public class DriverProvider implements Provider<Driver> {
    /** The license. */
    @Inject
    public License license;
    
    /**  Get the driver. */
    public Driver get() {
        return new Driver(license);
    }
}

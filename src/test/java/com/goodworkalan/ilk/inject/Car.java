package com.goodworkalan.ilk.inject;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A car.
 *
 * @author Alan Gutierrez
 */
public class Car implements Vehicle {
    /** The driver seat. */
    public Seat driverSeat;
    
    /** The passenger seat. */
    public Seat passengerSeat;
    
    /** The driver. */
    public Driver driver;

    /**
     * The car.
     * 
     * @param driver
     *            The driver.
     * @param passenger
     *            The passenger.
     */
    @Inject
    public Car(Seat driver, Provider<Seat> passenger) {
        this.driverSeat = driver;
        this.passengerSeat = passenger.get();
    }
    
    /**
     * Set the driver.
     * 
     * @param driver
     *            The driver.
     */
    @Inject
    public void setDriver(@Licensed Driver driver) {
        this.driver = driver;
    }
}


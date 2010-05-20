package com.goodworkalan.ilk.inject;

import javax.inject.Inject;
import javax.inject.Provider;

public class Car implements Vehicle {
    public Seat driverSeat;
    
    public Seat passengerSeat;
    
    public Driver driver;
    
    @Inject
    public Car(Seat driver, Provider<Seat> passenger) {
        this.driverSeat = driver;
        this.passengerSeat = passenger.get();
    }
    
    @Inject
    public void setDriver(@Licensed Driver driver) {
        this.driver = driver;
    }
}


package com.goodworkalan.ilk.inject;

import javax.inject.Inject;
import javax.inject.Provider;

public class Car implements Vehicle {
    public Seat driverSeat;
    
    public Seat passengerSeat;
    
    @Inject
    public Car(Seat driver, Provider<Seat> passenger) {
        this.driverSeat = driver;
        this.passengerSeat = passenger.get();
    }
}

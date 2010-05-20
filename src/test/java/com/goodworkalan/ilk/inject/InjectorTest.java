package com.goodworkalan.ilk.inject;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.Ilk;

public class InjectorTest {
    @Test
    public void everything() {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            public void build() {
                implementation(ilk(Car.class), ilk(Vehicle.class), null,  null);
            }
        });
        newInjector.module(new InjectorBuilder() {
            public void build() {
                implementation(ilk(Car.class), ilk(Vehicle.class), null,  null);
            }
        });
        Injector injector = newInjector.newInjector();
        Vehicle vehicle = injector.instance(new Ilk<Vehicle>(Vehicle.class), null);
        assertTrue(vehicle instanceof Car);
        assertNotNull(((Car) vehicle).driverSeat);
    }
}

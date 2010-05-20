package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

public class DriverProvider implements Provider<Driver> {
    public Driver get() {
        return new Driver();
    }
}

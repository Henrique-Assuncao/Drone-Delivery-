package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneTest {

    @Test
    void shouldRejectNullIdentifier() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Drone(null, 10.0, 100.0)
        );

        assertEquals("identifier must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankIdentifier() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Drone(" ", 10.0, 100.0)
        );

        assertEquals("identifier must not be blank", exception.getMessage());
    }

    @Test
    void shouldCreateDroneWithRequiredFields() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);

        assertEquals("DRONE-1", drone.identifier());
        assertEquals(10.0, drone.maxWeightCapacity());
        assertEquals(100.0, drone.maxRange());
        assertEquals(100.0, drone.batteryLevel());
        assertEquals(1.0, drone.batteryConsumptionPerDistanceUnit());
        assertEquals(20.0, drone.minimumReturnBattery());
        assertEquals(60.0, drone.speed());
        assertEquals(10.0, drone.chargingRate());
    }

    @Test
    void shouldCreateDroneWithBatteryFields() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0, 75.0, 1.5, 25.0, 2.0, 12.0);

        assertEquals("DRONE-1", drone.identifier());
        assertEquals(10.0, drone.maxWeightCapacity());
        assertEquals(100.0, drone.maxRange());
        assertEquals(75.0, drone.batteryLevel());
        assertEquals(1.5, drone.batteryConsumptionPerDistanceUnit());
        assertEquals(25.0, drone.minimumReturnBattery());
        assertEquals(2.0, drone.speed());
        assertEquals(12.0, drone.chargingRate());
    }

    @Test
    void shouldRejectInvalidWeightCapacity() {
        IllegalArgumentException zeroCapacity = assertThrows(
                IllegalArgumentException.class,
                () -> new Drone("DRONE-1", 0.0, 100.0)
        );

        IllegalArgumentException negativeCapacity = assertThrows(
                IllegalArgumentException.class,
                () -> new Drone("DRONE-1", -1.0, 100.0)
        );

        assertEquals("maxWeightCapacity must be greater than zero", zeroCapacity.getMessage());
        assertEquals("maxWeightCapacity must be greater than zero", negativeCapacity.getMessage());
    }

    @Test
    void shouldRejectInvalidRange() {
        IllegalArgumentException zeroRange = assertThrows(
                IllegalArgumentException.class,
                () -> new Drone("DRONE-1", 10.0, 0.0)
        );

        IllegalArgumentException negativeRange = assertThrows(
                IllegalArgumentException.class,
                () -> new Drone("DRONE-1", 10.0, -1.0)
        );

        assertEquals("maxRange must be greater than zero", zeroRange.getMessage());
        assertEquals("maxRange must be greater than zero", negativeRange.getMessage());
    }

    @Test
    void shouldRejectInvalidBatteryLevel() {
        InvalidInputException negativeBattery = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, -0.1, 1.0, 20.0, 1.0, 10.0)
        );

        InvalidInputException batteryAboveLimit = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, 100.1, 1.0, 20.0, 1.0, 10.0)
        );

        assertEquals("batteryLevel must be between 0 and 100", negativeBattery.getMessage());
        assertEquals("batteryLevel must be between 0 and 100", batteryAboveLimit.getMessage());
    }

    @Test
    void shouldRejectInvalidBatteryConsumption() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, 100.0, 0.0, 20.0, 1.0, 10.0)
        );

        assertEquals("batteryConsumptionPerDistanceUnit must be greater than zero", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidMinimumReturnBattery() {
        InvalidInputException negativeBattery = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, 100.0, 1.0, -0.1, 1.0, 10.0)
        );

        InvalidInputException batteryAboveLimit = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, 100.0, 1.0, 100.1, 1.0, 10.0)
        );

        assertEquals("minimumReturnBattery must be between 0 and 100", negativeBattery.getMessage());
        assertEquals("minimumReturnBattery must be between 0 and 100", batteryAboveLimit.getMessage());
    }

    @Test
    void shouldRejectInvalidSpeed() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, 100.0, 1.0, 20.0, 0.0, 10.0)
        );

        assertEquals("speed must be greater than zero", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidChargingRate() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Drone("DRONE-1", 10.0, 100.0, 100.0, 1.0, 20.0, 1.0, 0.0)
        );

        assertEquals("chargingRate must be greater than zero", exception.getMessage());
    }

    @Test
    void shouldCalculateBatteryRequiredForDistanceIncludingSafeReturnReserve() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0, 80.0, 1.5, 25.0, 1.0, 10.0);

        assertEquals(40.0, drone.batteryRequiredForDistance(10.0));
    }

    @Test
    void shouldCheckWhetherTripCanBeCompletedWithSafeReturn() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0, 30.0, 1.0, 20.0, 1.0, 10.0);

        assertTrue(drone.canCompleteTripWithSafeReturn(10.0));
        assertFalse(drone.canCompleteTripWithSafeReturn(10.1));
    }

    @Test
    void shouldRejectNegativeDistanceWhenCalculatingBatteryRequirement() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> drone.batteryRequiredForDistance(-0.1)
        );

        assertEquals("distance must not be negative", exception.getMessage());
    }

    @Test
    void shouldCheckWhetherOrderWeightIsSupported() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);

        Order supportedOrder = new Order("ORDER-1", new Coordinate(1.0, 1.0), 10.0, Priority.HIGH);
        Order unsupportedOrder = new Order("ORDER-2", new Coordinate(1.0, 1.0), 10.1, Priority.HIGH);

        assertTrue(drone.supportsWeightOf(supportedOrder));
        assertFalse(drone.supportsWeightOf(unsupportedOrder));
    }

    @Test
    void shouldRejectNullOrderWhenCheckingWeightSupport() {
        Drone drone = new Drone("DRONE-1", 10.0, 100.0);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> drone.supportsWeightOf(null)
        );

        assertEquals("order must not be null", exception.getMessage());
    }
}

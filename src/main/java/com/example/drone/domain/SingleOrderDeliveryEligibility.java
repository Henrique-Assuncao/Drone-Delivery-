package com.example.drone.domain;

import com.example.drone.exception.*;

public class SingleOrderDeliveryEligibility {

    private static final Coordinate BASE_LOCATION = new Coordinate(0.0, 0.0);

    public boolean canServe(Drone drone, Order order) {
        if (drone == null) {
            throw new InvalidInputException("drone must not be null");
        }

        if (order == null) {
            throw new InvalidInputException("order must not be null");
        }

        double roundTripDistance = BASE_LOCATION.distanceTo(order.location()) * 2;

        return drone.supportsWeightOf(order) && roundTripDistance <= drone.maxRange();
    }
}

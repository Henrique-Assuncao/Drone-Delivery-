package com.example.drone.domain;

import com.example.drone.exception.*;

import java.util.List;

public record TripPlan(List<Trip> trips, List<UnallocatedOrder> unallocatedOrders) {

    public TripPlan {
        if (trips == null) {
            throw new InvalidInputException("trips must not be null");
        }

        if (unallocatedOrders == null) {
            throw new InvalidInputException("unallocatedOrders must not be null");
        }

        for (Trip trip : trips) {
            if (trip == null) {
                throw new InvalidInputException("trips must not contain null");
            }
        }

        for (UnallocatedOrder unallocatedOrder : unallocatedOrders) {
            if (unallocatedOrder == null) {
                throw new InvalidInputException("unallocatedOrders must not contain null");
            }
        }

        trips = List.copyOf(trips);
        unallocatedOrders = List.copyOf(unallocatedOrders);
    }
}

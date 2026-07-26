package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

import java.util.List;

public record PersistedTripPlan(List<TripEntity> trips, List<PersistedUnallocatedOrder> unallocatedOrders) {

    public PersistedTripPlan {
        trips = List.copyOf(trips);
        unallocatedOrders = List.copyOf(unallocatedOrders);
    }
}

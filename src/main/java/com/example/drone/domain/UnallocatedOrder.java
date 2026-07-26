package com.example.drone.domain;

import com.example.drone.exception.*;

public record UnallocatedOrder(Order order, String reason) {

    public UnallocatedOrder {
        if (order == null) {
            throw new InvalidInputException("order must not be null");
        }

        if (reason == null || reason.isBlank()) {
            throw new InvalidInputException("reason must not be blank");
        }
    }
}

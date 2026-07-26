package com.example.drone.service;

import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;

public record PersistedUnallocatedOrder(OrderEntity order, String reason) {
}

package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityTest {

    @Test
    void shouldContainAllSupportedPriorities() {
        assertArrayEquals(
                new Priority[]{Priority.LOW, Priority.MEDIUM, Priority.HIGH},
                Priority.values()
        );
    }

    @Test
    void shouldRepresentPriorityByName() {
        assertEquals("LOW", Priority.LOW.name());
        assertEquals("MEDIUM", Priority.MEDIUM.name());
        assertEquals("HIGH", Priority.HIGH.name());
    }
}

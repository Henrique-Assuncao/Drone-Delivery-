package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewTest {

    @Test
    void shouldCreateValidReview() {
        Review review = new Review(5, "Entrega excelente", "O pedido chegou antes do previsto.");

        assertEquals(5, review.stars());
        assertEquals("Entrega excelente", review.title());
        assertEquals("O pedido chegou antes do previsto.", review.feedback());
    }

    @Test
    void shouldRejectStarsBelowMinimum() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Review(0, "Entrega ruim", "A nota nao pode ser zero.")
        );

        assertEquals("stars must be between 1 and 5", exception.getMessage());
    }

    @Test
    void shouldRejectStarsAboveMaximum() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Review(6, "Entrega excelente", "A nota nao pode passar de cinco.")
        );

        assertEquals("stars must be between 1 and 5", exception.getMessage());
    }

    @Test
    void shouldRejectBlankTitle() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Review(4, " ", "Feedback valido.")
        );

        assertEquals("title must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankFeedback() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> new Review(4, "Titulo valido", "")
        );

        assertEquals("feedback must not be blank", exception.getMessage());
    }
}

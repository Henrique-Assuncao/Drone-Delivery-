package com.example.drone;

import com.example.drone.controller.*;
import com.example.drone.domain.*;
import com.example.drone.exception.*;
import com.example.drone.persistence.*;
import com.example.drone.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private MockMvc mockMvc;
    private InMemoryReviewStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryReviewStorage();
        ReviewRegistrationService registrationService = new ReviewRegistrationService(storage);
        ReviewQueryService queryService = new ReviewQueryService(storage);

        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(registrationService, queryService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stars": 5,
                                  "title": "Entrega excelente",
                                  "feedback": "O pedido chegou antes do previsto."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.title").value("Entrega excelente"))
                .andExpect(jsonPath("$.feedback").value("O pedido chegou antes do previsto."))
                .andExpect(jsonPath("$.reviewedAt").exists());
    }

    @Test
    void shouldRejectReviewWithoutRequestBody() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request body is invalid"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectReviewWithStarsBelowMinimum() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stars": 0,
                                  "title": "Entrega ruim",
                                  "feedback": "A nota nao pode ser zero."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("stars must be between 1 and 5"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectReviewWithStarsAboveMaximum() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stars": 6,
                                  "title": "Entrega excelente",
                                  "feedback": "A nota nao pode passar de cinco."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("stars must be between 1 and 5"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectReviewWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stars": 4,
                                  "title": " ",
                                  "feedback": "Feedback valido."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title must not be blank"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldRejectReviewWithBlankFeedback() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stars": 4,
                                  "title": "Titulo valido",
                                  "feedback": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("feedback must not be blank"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    @Test
    void shouldListReviews() throws Exception {
        storage.save(new ReviewEntity(null, 5, "Entrega excelente", "Chegou antes do previsto."));
        storage.save(new ReviewEntity(null, 3, "Entrega aceitavel", "Chegou no prazo."));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].stars").value(5))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].stars").value(3));
    }

    @Test
    void shouldFindReviewById() throws Exception {
        storage.save(new ReviewEntity(null, 5, "Entrega excelente", "Chegou antes do previsto."));

        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stars").value(5))
                .andExpect(jsonPath("$.title").value("Entrega excelente"))
                .andExpect(jsonPath("$.feedback").value("Chegou antes do previsto."));
    }

    @Test
    void shouldReturnNotFoundWhenReviewDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/reviews/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("review not found"))
                .andExpect(content().string(not(containsString("trace"))));
    }

    private static class InMemoryReviewStorage implements ReviewStorage {

        private final Map<Long, ReviewEntity> reviewsById = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public List<ReviewEntity> findAll() {
            return new ArrayList<>(reviewsById.values());
        }

        @Override
        public Optional<ReviewEntity> findById(Long id) {
            return Optional.ofNullable(reviewsById.get(id));
        }

        @Override
        public ReviewEntity save(ReviewEntity review) {
            ReviewEntity savedReview = new ReviewEntity(
                    nextId++,
                    review.getStars(),
                    review.getTitle(),
                    review.getFeedback(),
                    review.getReviewedAt()
            );

            reviewsById.put(savedReview.getId(), savedReview);

            return savedReview;
        }
    }
}

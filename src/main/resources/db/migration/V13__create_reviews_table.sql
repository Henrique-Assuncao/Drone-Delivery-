CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    stars INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    feedback TEXT NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT reviews_stars_between_one_and_five CHECK (stars BETWEEN 1 AND 5),
    CONSTRAINT reviews_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT reviews_feedback_not_blank CHECK (length(trim(feedback)) > 0)
);

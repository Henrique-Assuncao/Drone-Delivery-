CREATE TABLE obstacles (
    id BIGSERIAL PRIMARY KEY,
    center_x DOUBLE PRECISION NOT NULL,
    center_y DOUBLE PRECISION NOT NULL,
    radius DOUBLE PRECISION NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT obstacles_radius_positive CHECK (radius > 0)
);

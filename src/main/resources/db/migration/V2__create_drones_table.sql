CREATE TABLE drones (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(100) NOT NULL UNIQUE,
    max_weight_capacity DOUBLE PRECISION NOT NULL,
    max_range DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT drones_max_weight_capacity_positive CHECK (max_weight_capacity > 0),
    CONSTRAINT drones_max_range_positive CHECK (max_range > 0)
);

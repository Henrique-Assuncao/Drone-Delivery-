CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    drone_id BIGINT NOT NULL REFERENCES drones(id),
    status VARCHAR(30) NOT NULL,
    total_weight DOUBLE PRECISION NOT NULL,
    total_distance DOUBLE PRECISION NOT NULL,
    CONSTRAINT trips_total_weight_non_negative CHECK (total_weight >= 0),
    CONSTRAINT trips_total_distance_non_negative CHECK (total_distance >= 0)
);

CREATE TABLE trip_orders (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    route_position INTEGER NOT NULL,
    CONSTRAINT trip_orders_route_position_non_negative CHECK (route_position >= 0),
    CONSTRAINT trip_orders_trip_order_unique UNIQUE (trip_id, order_id),
    CONSTRAINT trip_orders_trip_route_position_unique UNIQUE (trip_id, route_position)
);

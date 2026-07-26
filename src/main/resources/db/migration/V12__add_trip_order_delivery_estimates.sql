ALTER TABLE trip_orders
    ADD COLUMN estimated_delivery_time DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE trip_orders
    ADD CONSTRAINT trip_orders_estimated_delivery_time_non_negative
    CHECK (estimated_delivery_time >= 0);

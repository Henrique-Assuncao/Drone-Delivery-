ALTER TABLE trip_orders
    ADD COLUMN delivery_confirmation_requested_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE trip_orders
    ADD COLUMN delivery_failed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE trip_orders
    ADD COLUMN delivery_failure_reason VARCHAR(255);

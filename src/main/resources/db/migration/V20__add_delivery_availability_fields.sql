ALTER TABLE orders
    ADD COLUMN confirmed_delivery_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE trip_orders
    ADD COLUMN availability_notified_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE trip_orders
    ADD COLUMN availability_confirmed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE orders
    ADD CONSTRAINT orders_identifier_unique UNIQUE (identifier);

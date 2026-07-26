ALTER TABLE orders
    ALTER COLUMN delivery_confirmation_code TYPE VARCHAR(100);

UPDATE orders
SET delivery_confirmation_code = identifier;

ALTER TABLE orders
    ADD COLUMN delivery_confirmation_code VARCHAR(16);

UPDATE orders
SET delivery_confirmation_code = UPPER(SUBSTRING(MD5(id::text || identifier), 1, 6));

ALTER TABLE orders
    ALTER COLUMN delivery_confirmation_code SET NOT NULL;

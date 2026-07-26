ALTER TABLE orders
    ADD COLUMN status_reason VARCHAR(255);

UPDATE orders
SET status_reason = 'Pedido não alocado no planejamento atual.'
WHERE status = 'UNALLOCATED';

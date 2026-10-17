-- A table can only be deleted once no ACTIVE hold/reservation blocks it (service-layer pre-check).
-- But ANY row that ever referenced the table — including historical, non-blocking ones (expired
-- holds, past-dated or cancelled reservations) — still needs its table_id resolvable to NULL for
-- the delete to actually succeed; otherwise Hibernate's flush-time consistency check (or, absent
-- that, the raw FK constraint) rejects the delete regardless of the pre-check's conclusion.
ALTER TABLE reservation
    ALTER COLUMN table_id DROP NOT NULL;

ALTER TABLE reservation
    DROP CONSTRAINT reservation_table_id_fkey;

ALTER TABLE reservation
    ADD CONSTRAINT reservation_table_id_fkey FOREIGN KEY (table_id) REFERENCES restaurant_table (id) ON DELETE SET NULL;

ALTER TABLE table_hold
    ALTER COLUMN table_id DROP NOT NULL;

ALTER TABLE table_hold
    DROP CONSTRAINT table_hold_table_id_fkey;

ALTER TABLE table_hold
    ADD CONSTRAINT table_hold_table_id_fkey FOREIGN KEY (table_id) REFERENCES restaurant_table (id) ON DELETE SET NULL;

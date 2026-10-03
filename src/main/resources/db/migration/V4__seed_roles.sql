ALTER TABLE role
    ALTER COLUMN name SET NOT NULL,
    ADD CONSTRAINT uq_role_name UNIQUE (name);

INSERT INTO role (name) VALUES
    ('ADMIN'),
    ('RESTAURANT_USER'),
    ('CUSTOMER');

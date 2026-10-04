ALTER TABLE item_type
    ALTER COLUMN name SET NOT NULL,
    ADD CONSTRAINT uq_item_type_name UNIQUE (name);

INSERT INTO item_type (name) VALUES
    ('APPETIZER'),
    ('MAIN_COURSE'),
    ('DESSERT'),
    ('BEVERAGE');

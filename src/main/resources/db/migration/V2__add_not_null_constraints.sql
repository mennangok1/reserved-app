ALTER TABLE restaurant_table
    ALTER COLUMN restaurant_id SET NOT NULL;

ALTER TABLE menu_item
    ALTER COLUMN item_type_id SET NOT NULL,
    ALTER COLUMN restaurant_id SET NOT NULL;

ALTER TABLE reservation
    ALTER COLUMN table_id SET NOT NULL,
    ALTER COLUMN customer_user_id SET NOT NULL;

ALTER TABLE table_hold
    ALTER COLUMN table_id SET NOT NULL,
    ALTER COLUMN customer_id SET NOT NULL;

ALTER TABLE restaurant_user
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN restaurant_id SET NOT NULL;

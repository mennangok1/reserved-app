ALTER TABLE restaurant_table ADD COLUMN label VARCHAR(255);

INSERT INTO action (name, description) VALUES
    ('RESTAURANT_TABLE_CREATE', 'Create a table for a restaurant'),
    ('RESTAURANT_TABLE_READ', 'View a restaurant''s own tables'),
    ('RESTAURANT_TABLE_UPDATE', 'Update a restaurant table'),
    ('RESTAURANT_TABLE_DELETE', 'Delete a restaurant table');

INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id
FROM role r, action a
WHERE r.name = 'RESTAURANT_USER'
  AND a.name IN ('RESTAURANT_TABLE_CREATE', 'RESTAURANT_TABLE_READ', 'RESTAURANT_TABLE_UPDATE', 'RESTAURANT_TABLE_DELETE');

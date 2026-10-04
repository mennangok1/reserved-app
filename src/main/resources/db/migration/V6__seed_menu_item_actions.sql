ALTER TABLE action
    ALTER COLUMN name SET NOT NULL,
    ADD CONSTRAINT uq_action_name UNIQUE (name);

INSERT INTO action (name, description) VALUES
    ('MENU_ITEM_CREATE', 'Create a menu item for a restaurant'),
    ('MENU_ITEM_READ', 'View a restaurant''s menu items'),
    ('MENU_ITEM_UPDATE', 'Update a menu item'),
    ('MENU_ITEM_DELETE', 'Delete a menu item');

-- RESTAURANT_USER manages its own menu end to end.
INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id
FROM role r, action a
WHERE r.name = 'RESTAURANT_USER'
  AND a.name IN ('MENU_ITEM_CREATE', 'MENU_ITEM_READ', 'MENU_ITEM_UPDATE', 'MENU_ITEM_DELETE');

-- ADMIN and CUSTOMER only ever view menus (per CLAUDE.md role definitions).
INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id
FROM role r, action a
WHERE r.name IN ('ADMIN', 'CUSTOMER')
  AND a.name = 'MENU_ITEM_READ';

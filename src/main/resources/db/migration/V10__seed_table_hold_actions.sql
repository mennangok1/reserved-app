INSERT INTO action (name, description) VALUES
    ('TABLE_HOLD_CREATE', 'Hold a restaurant table for a short period'),
    ('TABLE_HOLD_DELETE', 'Release a held restaurant table');

INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id
FROM role r, action a
WHERE r.name = 'CUSTOMER'
  AND a.name IN ('TABLE_HOLD_CREATE', 'TABLE_HOLD_DELETE');

INSERT INTO action (name, description) VALUES
    ('RESTAURANT_READ', 'Browse restaurants and view a restaurant''s public menu');

INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id
FROM role r, action a
WHERE r.name IN ('ADMIN', 'CUSTOMER')
  AND a.name = 'RESTAURANT_READ';

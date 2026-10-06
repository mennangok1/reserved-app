INSERT INTO action (name, description) VALUES
    ('RESERVATION_CREATE', 'Create a reservation by consuming a table hold'),
    ('RESERVATION_READ', 'View own reservations'),
    ('RESERVATION_CANCEL', 'Cancel own reservation');

INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id
FROM role r, action a
WHERE r.name = 'CUSTOMER'
  AND a.name IN ('RESERVATION_CREATE', 'RESERVATION_READ', 'RESERVATION_CANCEL');

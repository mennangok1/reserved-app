-- ADMIN seed user. CLAUDE.md: ADMIN has no self-registration endpoint (unlike CUSTOMER/
-- RESTAURANT_USER), so it must be seeded directly. Password is 'password123' — the same
-- DEFAULT_PASSWORD every other seeded/test account in this project uses — hashed with BCrypt
-- cost 10 to match SecurityConfig.passwordEncoder(). Verified with Spring Security's own
-- BCryptPasswordEncoder (.matches("password123", hash) == true) before being committed here.
-- Local learning project only (CLAUDE.md: "fully local, never production") — a real
-- deployment would never hardcode an admin credential into a migration.
INSERT INTO users (name, email, password, phone_number, role_id)
SELECT 'Admin', 'admin@reserved.local',
       '$2y$10$DqBlClU4tVnggdOguP9lxOjNOKkqqHe4Ioq5Iow5gGlAQZ5OrKrbq',
       NULL, r.id
FROM role r
WHERE r.name = 'ADMIN';

-- New actions for this slice.
-- RESERVATION_READ_ALL is deliberately its OWN action, not a reuse of V11's RESERVATION_READ:
-- RESERVATION_READ is already granted to CUSTOMER (for their own reservations only). Reservation
-- rows contain another person's booking data, unlike restaurant/menu data (which is intentionally
-- public, so sharing RESTAURANT_READ/MENU_ITEM_READ between ADMIN and CUSTOMER is safe). Reusing
-- RESERVATION_READ here would let any CUSTOMER call GET /restaurants/{id}/reservations and see
-- every other customer's bookings for that restaurant — a real cross-customer data leak, not
-- just an inconsistency. Keeping it a separate, ADMIN-only action closes that off entirely.
INSERT INTO action (name, description) VALUES
    ('RESERVATION_READ_ALL', 'View all reservations for any restaurant'),
    ('USER_READ', 'View public info of all users'),
    ('ROLE_ACTION_READ', 'View the action catalog and current role-to-action grants'),
    ('ROLE_ACTION_CREATE', 'Grant an action to a role'),
    ('ROLE_ACTION_DELETE', 'Revoke an action from a role');

-- All five are ADMIN-only: this is the Permission Management surface itself (plus the
-- cross-restaurant reservation view), so none of this is delegatable to any other role in MVP.
INSERT INTO role_action (role_id, action_id)
SELECT r.id, a.id FROM role r, action a
WHERE r.name = 'ADMIN'
  AND a.name IN ('RESERVATION_READ_ALL', 'USER_READ', 'ROLE_ACTION_READ', 'ROLE_ACTION_CREATE', 'ROLE_ACTION_DELETE');

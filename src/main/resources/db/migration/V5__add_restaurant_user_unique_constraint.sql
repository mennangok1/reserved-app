-- Enforces the @OneToOne (User -> RestaurantUser) relationship at the DB level:
-- a user can manage at most one restaurant.
ALTER TABLE restaurant_user
    ADD CONSTRAINT uq_restaurant_user_user_id UNIQUE (user_id);

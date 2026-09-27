-- No FK dependencies
CREATE TABLE role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255)
);

CREATE TABLE action (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(255)
);

CREATE TABLE restaurant (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(255)
);

CREATE TABLE item_type (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255)
);

-- Depends on: role, action
CREATE TABLE role_action (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id   BIGINT NOT NULL REFERENCES role (id),
    action_id BIGINT NOT NULL REFERENCES action (id),
    CONSTRAINT uq_role_action_role_id_action_id UNIQUE (role_id, action_id)
);

-- Depends on: role
CREATE TABLE users (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    role_id      BIGINT NOT NULL REFERENCES role (id)
);

-- Depends on: restaurant
CREATE TABLE restaurant_table (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    capacity      BIGINT,
    restaurant_id BIGINT REFERENCES restaurant (id)
);

-- Depends on: item_type, restaurant
CREATE TABLE menu_item (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255),
    description   VARCHAR(255),
    price         DOUBLE PRECISION,
    menu_order    BIGINT,
    item_type_id  BIGINT REFERENCES item_type (id),
    restaurant_id BIGINT REFERENCES restaurant (id)
);

-- Depends on: users, restaurant
CREATE TABLE restaurant_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id),
    restaurant_id BIGINT REFERENCES restaurant (id)
);

-- Depends on: users
CREATE TABLE customer_user (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id)
);

-- Depends on: restaurant_table, customer_user
CREATE TABLE reservation (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    start_date        TIMESTAMP,
    end_date          TIMESTAMP,
    table_id          BIGINT REFERENCES restaurant_table (id),
    customer_user_id  BIGINT REFERENCES customer_user (id),
    status            VARCHAR(255)
);

-- Depends on: restaurant_table, customer_user
CREATE TABLE table_hold (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expires_at  TIMESTAMP,
    table_id    BIGINT REFERENCES restaurant_table (id),
    customer_id BIGINT REFERENCES customer_user (id)
);

# Reserved

A restaurant reservation and menu management platform: restaurants manage their menus, seating, and reservations; customers search restaurants, browse menus, and book tables in real time.

This is a learning project focused on backend architecture practice (authorization design, concurrency handling, schema modeling) rather than a production SaaS. It was built as a series of vertical slices, each preceded by an ADR in [`docs/decisions/`](docs/decisions/).

## Features

- **Restaurant discovery** — customers search restaurants and browse menus without an account.
- **Reservations with a soft lock** — selecting a table holds it exclusively for 5 minutes (released on expiry or on leaving the page), preventing double-booking without taking a database-level lock.
- **Menu & seating management** — restaurant users maintain their own restaurant's menu items and physical tables.
- **Dynamic permission management** — admins grant/revoke role permissions at runtime from a Permission Management screen, no redeploy required.
- **Scoped, KVKK-aware data access** — restaurant users see only their own restaurant's data and the other restaurant users linked to it; customers never see other customers', restaurant users', or admins' data.

## Roles

| Role | Can do |
|---|---|
| **Admin** | View public info of all users; view any restaurant's menu and reservations; manage role permissions dynamically |
| **Restaurant User** | Manage their own restaurant's menu and seating plan; view only their own restaurant's data and its linked restaurant users |
| **Customer** | Search restaurants, view menus, make reservations; no visibility into other customers, restaurant users, or admins |

A restaurant can have multiple restaurant users (many-to-one via a `RestaurantUser` join entity). MVP ships a single flat restaurant-user type — no owner/staff hierarchy yet.

## Architecture

### Authorization: hybrid role-based + resource-based

The `Role` enum (`ADMIN`, `RESTAURANT_USER`, `CUSTOMER`) decides broad capability. Restaurant ownership (the `RestaurantUser` table) decides scope. Resource checks run in the service layer, not just via annotations — so "can a restaurant user edit *this* menu item" is a code-level check against restaurant ownership, not a blanket `@PreAuthorize("hasRole(...)")`.

### Runtime-configurable RBAC

Beyond the three roles, `Role` / `Action` / `RoleAction` tables let admins change what a role can do without a redeploy — deliberately more than the MVP strictly needs, built as practice for a real pain point (enum-only roles requiring redeploys for permission changes). Role → permission lookups are cached with Spring's `@Cacheable` (no Redis in MVP). This table answers "can this role do X" only; "on which restaurant" stays a separate, resource-based check.

### Table modeling

Physical tables are individual rows (`restaurant_table(id, restaurant_id, capacity, label)`), never aggregated counts — so a restaurant with "4 tables of 4 and 3 tables of 6" is 7 distinct rows, each independently holdable and bookable.

### Reservation lock (application-level, not DB-level)

When a customer selects a table, a `TableHold` row is created with a 5-minute `expiresAt`, exclusive to that customer. It's released on expiry or when the customer leaves the page. This is intentionally **not** a long-held `SELECT ... FOR UPDATE` — holds are a separate entity decoupled from `Reservation`, keeping the lock logic out of the database transaction layer.

### Deferred by design

- **Automatic table assignment** — customers pick a specific table; matching algorithms are a later version.
- **File storage** (profile photos, menu images) — its own late-stage vertical slice. When built: external object storage (MinIO locally, S3-compatible in prod-shape), with the DB holding only `File` metadata (path, content-type, size, uploader) — never a BLOB.
- **Admin audit logging** — planned, not in MVP.
- **Owner/staff hierarchy** within a restaurant.

## Tech stack

- **Backend:** Java 21, Spring Boot (Web, JPA/Hibernate, Security, Validation)
- **Database:** PostgreSQL, Flyway migrations (`ddl-auto=validate` — schema changes only ever happen through versioned SQL, never Hibernate auto-DDL)
- **Frontend:** React
- **Infra:** Docker Compose (local only — no production deployment target)
- **Build:** Maven

## Project structure

```
src/main/java/com/mennangok1/reserved/
  user/              -- account identity shared by all roles
  customerUser/       restaurant/        restaurantUser/
  restaurantTable/     menuItem/          itemType/
  reservation/         tableHold/
  role/  action/  roleAction/            -- dynamic RBAC
src/main/resources/db/migration/        -- Flyway SQL, sequential (V1__, V2__, ...)
architecture/                           -- ADRs, ER diagram, MVP scope notes
```

## Getting started

### Prerequisites

- Java 21
- Docker / Docker Compose
- Node.js (for the React frontend)

### Run locally

```bash
# 1. Configure environment
cp .env.example .env
# edit .env with local DB credentials

# 2. Start PostgreSQL
docker compose up -d

# 3. Run the backend (applies Flyway migrations on boot)
./mvnw spring-boot:run

# 4. Run the frontend
cd frontend
npm install
npm run dev
```

The API starts on `http://localhost:8080`; the frontend dev server prints its own port on startup.

### Tests

```bash
./mvnw test
```

Integration tests use Testcontainers to spin up a real PostgreSQL instance rather than mocking the database.

## Database migrations

Schema changes go through Flyway, plain sequential SQL (`V1__create_core_schema.sql`, `V2__add_not_null_constraints.sql`, ...) in `src/main/resources/db/migration/`. Hibernate runs in `ddl-auto=validate` mode — it checks the schema matches the entities but never writes to it. Chosen over Liquibase for simplicity, since SQL was already a known tool.

## Decision records

Non-trivial decisions (concurrency, authorization, schema shape) are written up as short ADRs before implementation: what changed, why, alternatives considered, and known risk/debt. See [`docs/architecture`](docs/architecture/) for the MVP scope doc and per-issue notes, and `docs/decisions/` for the ADR log.

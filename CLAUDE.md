# CLAUDE.md

## Project

Restaurant reservation and menu management platform (learning project — architecture practice, not a production SaaS). Backend-first, built as vertical slices, each with its own ADR before implementation.

## Tech stack

- Java 21, Spring Boot (Web, JPA/Hibernate, Security, Validation)
- PostgreSQL, Flyway for migrations (`ddl-auto=validate`, never `update`)
- React (frontend)
- Docker / Docker Compose (local only, no prod deploy planned yet)
- Maven

## Roles (3)

- **ADMIN** — views public info of all users; views restaurant menus/reservations (no audit log yet, planned for later); manages permissions dynamically via a Permission Management screen.
- **RESTAURANT_USER** — manages their own restaurant's menu and seating plan; can view only their own restaurant's data and the Restaurant Users linked to it (KVKK: no cross-restaurant visibility, no sensitive data leakage between restaurant users). MVP has a single flat Restaurant User type — no owner/staff hierarchy yet. A restaurant can have multiple Restaurant Users (many-to-one via `RestaurantUser` join entity, not a direct FK on `User`).
- **CUSTOMER** — searches restaurants, views menus, makes reservations. No visibility into other customers, restaurant users, or admin data.

## Core architectural decisions (already made — do not relitigate without a new ADR)

1. **Authorization model:** hybrid role-based + resource-based. Role enum (`ADMIN`, `RESTAURANT_USER`, `CUSTOMER`) decides broad capability; restaurant ownership (`RestaurantUser` table) decides scope. Resource checks happen in code (service layer), not just annotations.
2. **RBAC tables (Role / Action / RoleAction):** intentionally added beyond MVP's strict needs, as a deliberate learning exercise (mirrors a real pain point at my job — enum-only roles requiring redeploys for permission changes). Must include: runtime permission checks (not compile-time `hasRole` strings), `@Cacheable` caching of role→permission lookups (no Redis in MVP), and a way to manage permissions dynamically. Resource-based (restaurant ownership) checks stay separate from this table — RBAC answers "can this role do X", not "on which restaurant".
3. **Table modeling:** individual rows per physical table (`Table(id, restaurantId, capacity, label)`), never aggregated counts (not "4 tables of capacity 4" as one row). This is what makes the hold/lock mechanism per-table.
4. **Reservation lock:** when a customer selects a table, it is held for 5 minutes, exclusively for that customer. Released on expiry or on leaving the page. **Application-level only** — never a DB-level lock (no long-held `SELECT ... FOR UPDATE` transactions). Implemented as a separate `TableHold` entity/table with `expiresAt`, decoupled from `Reservation`.
5. **Automatic table assignment:** out of MVP scope, deferred to a later version.
6. **File storage (profile photos, menu item images):** deferred to its own late-stage vertical slice, not mixed into earlier phases. When built: external object storage (MinIO locally, S3-compatible), DB holds only a `File` metadata row (path, content-type, size, uploader), never a BLOB.
7. **Migrations:** Flyway, plain SQL, sequential (`V1__...`, `V2__...`). Chosen over Liquibase for simplicity and because I already know SQL well from work.

## Working conventions for this session

- I'm a junior fullstack dev using this project to build architecture/system-design skills, not just to ship features. Practice ratio: favor doing over long explanations, but don't skip the reasoning behind non-trivial decisions.
- Work in vertical slices (auth → one role → one feature, end-to-end, deployable locally) rather than building all layers horizontally before anything works.
- Before implementing a non-trivial decision (concurrency, authorization, schema shape), surface the decision and trade-offs briefly so I can confirm or redirect — don't silently pick an approach.
- After a slice is done, help me draft the ADR entry for `docs/decisions/` (short format: what changed, why, alternatives considered, risk/debt) — but the final wording and judgment call is mine, not generated wholesale.
- Don't introduce new infrastructure (queues, Redis, new services) without flagging it as a scope decision first.
- This is a side project, fully local — never anything that touches or risks my employer's systems, repos, or data.

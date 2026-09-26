## MVP Scope

**3 Roles:** Admin, Restaurant User, Customer

**Admin permissions:** View public information of all users, view restaurant information (menu, reservation viewing). Audit logging for Admin viewing may be added in future versions. Can dynamically manage permissions via a Permission Management screen. Permissions should be cached using Spring `@Cacheable`; Redis is not needed for MVP.

**Restaurant User:** Edit the restaurant's menu, edit the restaurant's seating plan (in the MVP phase this will be handled via a list — e.g. information will be entered such as 4 tables of 4 people and 3 tables of 6 people, based on which customers can make reservations. Table information will be stored as separate rows in the DB. That is, not "Restaurant A has 4 tables of 4 people" but rather "Restaurant A — Table 1, 4-person; Table 2, 4-person…"). A Restaurant User can only view information belonging to their own restaurant and the Restaurant Users linked to their own restaurant (other Restaurant Users' information must be treated under KVKK [Turkish GDPR-equivalent data protection law], with care taken over information that must be kept confidential). In the MVP there is a single type of Restaurant User (a restaurant can have multiple users). In future versions a restaurant owner/staff hierarchy may be established.

**Note:** The Automatic Table Assignment mechanism will be added in a future version.

**Customer:** Restaurant search, browsing restaurant menus, and making reservations will be added. Cannot view information of other customers, Restaurant Users, or the Admin.

## Tech Stack

PostgreSQL, Java Spring Boot, React, Claude Code, Docker

## Reservation Lock

When a Customer selects a table for reservation, that table is locked for that customer for 5 minutes. This lock is released once the time expires, or earlier if the customer leaves the page. This locking mechanism must be handled at the application level, not the database level.
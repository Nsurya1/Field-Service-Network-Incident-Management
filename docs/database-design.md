# Database Design — Field Service Management

## 1. Engineers

Purpose: Store field engineer information.

* `id`: Primary key
* `name`: Engineer's full name
* `email`: Unique email address
* `phone`: Contact number
* `skill`: Primary technical skill
* `active`: Whether the engineer is active

## 2. Incidents

Purpose: Track network faults and service issues.

* `id`: Primary key
* `title`: Short incident title
* `description`: Detailed issue
* `priority`: LOW, MEDIUM, HIGH, CRITICAL
* `status`: OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
* `location`: Customer or equipment location
* `engineer_id`: Foreign key referencing engineers
* `created_at`: Incident creation timestamp
* `updated_at`: Last update timestamp

## 3. Service Visits

Purpose: Record work performed during field visits.

* `id`: Primary key
* `incident_id`: Foreign key referencing incidents
* `engineer_id`: Foreign key referencing engineers
* `visit_date`: Date and time of visit
* `work_notes`: Work performed
* `resolution_notes`: Findings or resolution details

## Relationships

* One engineer can be assigned many incidents.
* An incident may have no assigned engineer.
* One incident can have multiple service visits.
* One engineer can perform multiple service visits.

## Design Decisions

* Use generated numeric IDs as primary keys.
* Enforce unique engineer email addresses.
* Use controlled values for incident priority and status.
* Store timestamps for auditability.
* Use foreign keys to maintain referential integrity.

-- ============================================================
-- Integrated Food Supply & Nutrition Management
-- PostgreSQL setup. Run once as a superuser, e.g.:
--   psql -U postgres -f db/setup.sql
-- Creates one application user and one database per service.
-- Services never share tables; each owns its own database.
-- ============================================================

-- Application user (change the password before any real deployment)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'foodapp') THEN
        CREATE ROLE foodapp WITH LOGIN PASSWORD 'foodapp123';
    END IF;
END
$$;

-- CREATE DATABASE cannot run inside a DO block, so each is created
-- separately. If a database already exists, psql prints an error for
-- that line and carries on, which is safe to ignore.
CREATE DATABASE beneficiary_db  OWNER foodapp;
CREATE DATABASE food_db         OWNER foodapp;
CREATE DATABASE procurement_db  OWNER foodapp;
CREATE DATABASE inventory_db    OWNER foodapp;
CREATE DATABASE distribution_db OWNER foodapp;
CREATE DATABASE programme_db    OWNER foodapp;
CREATE DATABASE quality_db      OWNER foodapp;
CREATE DATABASE users_db        OWNER foodapp;
-- reporting-service is a stateless aggregator and needs no database.

GRANT ALL PRIVILEGES ON DATABASE beneficiary_db  TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE food_db         TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE procurement_db  TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE inventory_db    TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE distribution_db TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE programme_db    TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE quality_db      TO foodapp;
GRANT ALL PRIVILEGES ON DATABASE users_db        TO foodapp;

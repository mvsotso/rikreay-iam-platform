-- H2 test schema: create sequence for config version (matches Flyway V1 migration)
CREATE SEQUENCE IF NOT EXISTS config_version_seq START WITH 1 INCREMENT BY 1;

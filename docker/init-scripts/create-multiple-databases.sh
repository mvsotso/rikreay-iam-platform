#!/bin/bash
# =============================================================================
# Create multiple PostgreSQL databases on container init
# Each IAM microservice gets its own database (schema-per-service pattern)
# =============================================================================

set -e
set -u

function create_database() {
    local database=$1
    echo "Creating database '$database'..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'CREATE DATABASE $database'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$database')\gexec
        GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
    echo "Database '$database' created successfully."
}

# keycloak_db is created by POSTGRES_DB env var, skip it
# Create all IAM service databases
DATABASES="iam_core iam_tenant iam_audit iam_xroad iam_admin iam_monitoring iam_governance iam_developer iam_notification iam_config"

for db in $DATABASES; do
    create_database "$db"
done

echo "============================================="
echo "All IAM databases created successfully!"
echo "============================================="
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -c "\l"

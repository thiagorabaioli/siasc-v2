#!/bin/sh
# Corre uma única vez, no primeiro arranque do siac-postgres (volume vazio).
# Cria um schema e um role de login por serviço, com privilégios restritos
# ao seu próprio schema (ver docs/arquitetura.md §6).
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE SCHEMA IF NOT EXISTS siac_auth;
	CREATE SCHEMA IF NOT EXISTS siac_core;

	DO \$\$
	BEGIN
	    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'siac_auth_user') THEN
	        CREATE ROLE siac_auth_user LOGIN PASSWORD '${SIAC_AUTH_DB_PASSWORD}';
	    END IF;
	    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'siac_core_user') THEN
	        CREATE ROLE siac_core_user LOGIN PASSWORD '${SIAC_CORE_DB_PASSWORD}';
	    END IF;
	END
	\$\$;

	REVOKE ALL ON SCHEMA public FROM siac_auth_user, siac_core_user;

	GRANT USAGE, CREATE ON SCHEMA siac_auth TO siac_auth_user;
	GRANT USAGE, CREATE ON SCHEMA siac_core TO siac_core_user;

	ALTER ROLE siac_auth_user SET search_path = siac_auth;
	ALTER ROLE siac_core_user SET search_path = siac_core;
EOSQL

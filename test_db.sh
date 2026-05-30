#!/bin/bash
docker run --name pg_test -e POSTGRES_PASSWORD=pass -e POSTGRES_USER=user -e POSTGRES_DB=testdb -d pgvector/pgvector:16
sleep 5
docker cp /home/tomzxy/debian_server/fbu_chat/spring-api/fbu_chat/src/main/resources/db/migration/V3__rag_schema_enhancements.sql pg_test:/V3.sql
docker exec pg_test psql -U user -d testdb -c "CREATE EXTENSION unaccent;"
docker exec pg_test psql -U user -d testdb -f /V3.sql
docker rm -f pg_test

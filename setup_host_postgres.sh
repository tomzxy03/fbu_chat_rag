#!/usr/bin/env bash
# setup_host_postgres.sh
# Cài đặt PostgreSQL + pgvector trên HOST machine (chạy 1 lần)
# Hỗ trợ: Ubuntu/Debian
set -e

echo "=== 1. Cài PostgreSQL 16 ==="
sudo apt-get update -qq
sudo apt-get install -y postgresql-16 postgresql-server-dev-16 curl build-essential

echo "=== 2. Cài pgvector extension ==="
# Build từ source (cách phổ biến nhất)
cd /tmp
git clone --depth 1 https://github.com/pgvector/pgvector.git
cd pgvector
make
sudo make install
cd /tmp && rm -rf pgvector
echo "pgvector installed ✅"

echo "=== 3. Tạo user + database ==="
# Đọc từ .env nếu có
source /home/$(whoami)/debian_server/fbu_chat/.env 2>/dev/null || true
DB_USER=${POSTGRES_USER:-raguser}
DB_PASS=${POSTGRES_PASSWORD:-ragpass}
DB_NAME=${POSTGRES_DB:-ragdb}

sudo -u postgres psql <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$DB_USER') THEN
    CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';
  END IF;
END\$\$;
CREATE DATABASE $DB_NAME OWNER $DB_USER;
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
SQL

echo "=== 4. Bật pgvector + tạo schema ==="
sudo -u postgres psql -d "$DB_NAME" <<SQL
CREATE EXTENSION IF NOT EXISTS vector;
-- Cho phép user truy cập
GRANT USAGE ON SCHEMA public TO $DB_USER;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;
SQL

echo "=== 5. Chạy init.sql ==="
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ ! -f "$SCRIPT_DIR/postgres/init.sql" ]; then
    echo "Cảnh báo: Không tìm thấy file init.sql tại $SCRIPT_DIR/postgres/. Bỏ qua bước 5."
else
    psql -U "$DB_USER" -d "$DB_NAME" -h localhost -f "$SCRIPT_DIR/postgres/init.sql"
fi

echo "=== 6. Cho phép Docker containers kết nối ==="
PG_HBA=$(sudo -u postgres psql -t -c "SHOW hba_file;" | tr -d ' ')
PG_CONF=$(sudo -u postgres psql -t -c "SHOW config_file;" | tr -d ' ')

# Lắng nghe từ tất cả interfaces để Docker host-gateway hoạt động
if grep -q "^listen_addresses = '*'" "$PG_CONF"; then
    echo "listen_addresses đã được cấu hình là '*'. Bỏ qua."
else
    echo "Đang cấu hình listen_addresses = '*'..."
    sudo sed -i "s/^#\?listen_addresses\s*=.*/listen_addresses = '*'/" "$PG_CONF"
fi

# Cho phép local Docker subnet kết nối (172.17.0.0/16)
echo "host  $DB_NAME  $DB_USER  172.17.0.0/16  md5" | sudo tee -a "$PG_HBA"
echo "host  $DB_NAME  $DB_USER  127.0.0.1/32   md5" | sudo tee -a "$PG_HBA"

sudo systemctl restart postgresql

echo ""
echo "✅ PostgreSQL + pgvector đã sẵn sàng!"
echo "   DB: $DB_NAME | User: $DB_USER | Host: localhost:${POSTGRES_PORT:-5432}"
echo ""

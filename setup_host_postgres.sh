#!/usr/bin/env bash
# setup_host_postgres.sh
# Cài đặt PostgreSQL + pgvector + Unaccent trên HOST machine (chạy 1 lần)
# Hỗ trợ: Ubuntu/Debian
set -e

echo "=== 1. Cài PostgreSQL 16 ==="
sudo apt-get update -qq
sudo apt-get install -y postgresql-16 postgresql-server-dev-16 curl build-essential

echo "=== 2. Cài pgvector extension ==="
cd /tmp
git clone --depth 1 https://github.com/pgvector/pgvector.git
cd pgvector
make
sudo make install
cd /tmp && rm -rf pgvector
echo "pgvector installed!!"

echo "=== 3. Tạo user + database ==="
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

-- Drop và Re-create DB nếu bạn muốn làm sạch lịch sử phiên bản để chạy Migration gộp mới
-- Chú ý: Nếu muốn bảo lưu dữ liệu cũ, hãy comment dòng DROP DATABASE dưới đây.
DROP DATABASE IF EXISTS $DB_NAME;

CREATE DATABASE $DB_NAME OWNER $DB_USER;
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
SQL

echo "=== 4. Bật pgvector + unaccent + tạo immutable wrapper ==="
# Cần chạy với quyền superuser (postgres) để cài Extension và định nghĩa Hàm hệ thống
sudo -u postgres psql -d "$DB_NAME" <<SQL
-- Bật các extension cần thiết
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Tạo wrapper IMMUTABLE để bypass đặc tính STABLE của unaccent mặc định (Phục vụ GIN Index)
CREATE OR REPLACE FUNCTION public.immutable_unaccent(text) 
RETURNS text AS \$\$
  SELECT public.unaccent(\$1);
\$\$ LANGUAGE sql IMMUTABLE STRICT;

-- Phân quyền cho hàm vừa tạo để user của app (Spring Boot) gọi được khi chạy query
ALTER FUNCTION public.immutable_unaccent(text) OWNER TO $DB_USER;

-- Cho phép user truy cập
GRANT USAGE ON SCHEMA public TO $DB_USER;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;
SQL
echo "pgvector + unaccent + immutable_unaccent installed!!"

# echo "=== 5. Chạy init.sql ==="
# SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# if [ ! -f "$SCRIPT_DIR/postgres/init.sql" ]; then
#     echo "Cảnh báo: Không tìm thấy file init.sql tại $SCRIPT_DIR/postgres/. Bỏ qua bước 5."
# else
#     # Thêm PGPASSWORD để psql không hỏi mật khẩu khi chạy tự động
#     PGPASSWORD="$DB_PASS" psql -U "$DB_USER" -d "$DB_NAME" -h localhost -f "$SCRIPT_DIR/postgres/init.sql"
# fi

echo "=== 5. Cho phép Docker containers kết nối ==="
PG_HBA=$(sudo -u postgres psql -t -c "SHOW hba_file;" | tr -d ' ')
PG_CONF=$(sudo -u postgres psql -t -c "SHOW config_file;" | tr -d ' ')

if grep -q "^listen_addresses = '*'" "$PG_CONF"; then
    echo "listen_addresses đã được cấu hình là '*'. Bỏ qua."
else
    echo "Đang cấu hình listen_addresses = '*'..."
    sudo sed -i "s/^#\?listen_addresses\s*=.*/listen_addresses = '*'/" "$PG_CONF"
fi

# Tối ưu hóa: Cho phép dải IP nội bộ của Docker kết nối (Bao gồm cả bridge mặc định và custom mạng)
if ! grep -q "172.17.0.0/16" "$PG_HBA"; then
    echo "host  $DB_NAME  $DB_USER  172.17.0.0/16  md5" | sudo tee -a "$PG_HBA"
fi
if ! grep -q "172.18.0.0/16" "$PG_HBA"; then
    # Thêm dải 172.18.0.0/16 vì Docker Compose thường tự tạo network mới nằm ở dải này
    echo "host  $DB_NAME  $DB_USER  172.18.0.0/16  md5" | sudo tee -a "$PG_HBA"
fi
if ! grep -q "127.0.0.1/32" "$PG_HBA"; then
    echo "host  $DB_NAME  $DB_USER  127.0.0.1/32   md5" | sudo tee -a "$PG_HBA"
fi

sudo systemctl restart postgresql

echo ""
echo "PostgreSQL + pgvector + Unaccent đã sẵn sàng!!"
echo "   DB: $DB_NAME | User: $DB_USER | Host: localhost:${POSTGRES_PORT:-5432}"
echo ""
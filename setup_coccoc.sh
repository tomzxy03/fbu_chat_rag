#!/usr/bin/env bash
# setup_coccoc.sh
# Clone, build và cài đặt CocCoc Tokenizer cho FBU Chat
# Chạy 1 lần trên server trước khi docker compose build
#
# Sau khi chạy xong, cấu trúc trên host sẽ là:
#   ~/coccoc-tokenizer/   — source code
#   ~/coccoc-install/     — thư viện đã build (header, .so, dict, jar)
#   ~/coccoc-runtime/     — chỉ các .so file cần thiết lúc runtime (mount vào Docker)
#
# docker-compose.yml mount:
#   ~/coccoc-runtime                           → /app/libs              (native .so)
#   ~/coccoc-install/share/tokenizer/dicts     → /usr/share/tokenizer/dicts  (dict)
#   ~/coccoc-install/share/tokenizer/dicts     → /usr/local/share/tokenizer/dicts

set -euo pipefail

# ── Màu sắc log ──────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Cấu hình đường dẫn ───────────────────────────────────────────────────────
HOME_DIR="$(eval echo ~$(whoami))"
REPO_DIR="$HOME_DIR/coccoc-tokenizer"
INSTALL_DIR="$HOME_DIR/coccoc-install"
RUNTIME_DIR="$HOME_DIR/coccoc-runtime"
BUILD_DIR="$REPO_DIR/build"

# Path coccoc-tokenizer.jar sẽ được copy vào project để Maven build
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_DEST="$SCRIPT_DIR/spring-api/fbu_chat/libs/coccoc-tokenizer.jar"

# ── Bước 1: Kiểm tra dependencies ────────────────────────────────────────────
info "=== Bước 1: Kiểm tra dependencies ==="

MISSING_DEPS=()
for dep in git cmake make java; do
    command -v "$dep" &>/dev/null || MISSING_DEPS+=("$dep")
done

if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
    warn "Thiếu: ${MISSING_DEPS[*]}. Đang cài đặt..."
    sudo apt-get update -qq
    PKG_LIST=""
    for dep in "${MISSING_DEPS[@]}"; do
        case "$dep" in
            git)   PKG_LIST="$PKG_LIST git" ;;
            cmake) PKG_LIST="$PKG_LIST cmake" ;;
            make)  PKG_LIST="$PKG_LIST build-essential" ;;
            java)  PKG_LIST="$PKG_LIST openjdk-21-jdk-headless" ;;
        esac
    done
    sudo apt-get install -y $PKG_LIST
fi

info "Dependencies OK."

# ── Bước 2: Clone hoặc update repo ───────────────────────────────────────────
info "=== Bước 2: Clone coccoc-tokenizer ==="

if [ -d "$REPO_DIR/.git" ]; then
    warn "Repo đã tồn tại tại $REPO_DIR. Pulling latest..."
    git -C "$REPO_DIR" pull --ff-only
else
    git clone https://github.com/coccoc/coccoc-tokenizer.git "$REPO_DIR"
    info "Cloned thành công vào $REPO_DIR"
fi

# ── Bước 3: Build với Java binding ───────────────────────────────────────────
info "=== Bước 3: Build CocCoc Tokenizer (với Java binding) ==="

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

cmake \
    -DBUILD_JAVA=1 \
    -DCMAKE_INSTALL_PREFIX="$INSTALL_DIR" \
    -S "$REPO_DIR" \
    -B "$BUILD_DIR"

make -C "$BUILD_DIR" -j"$(nproc)"
make -C "$BUILD_DIR" install

info "Build và install hoàn tất → $INSTALL_DIR"

# ── Bước 4: Tạo thư mục runtime (chỉ .so) ────────────────────────────────────
info "=== Bước 4: Tạo thư mục runtime ==="

mkdir -p "$RUNTIME_DIR"

# Copy tất cả shared library (.so*) vào runtime dir
find "$INSTALL_DIR/lib" -name "*.so*" -exec cp -P {} "$RUNTIME_DIR/" \;

SO_COUNT=$(ls "$RUNTIME_DIR"/*.so* 2>/dev/null | wc -l)
if [ "$SO_COUNT" -eq 0 ]; then
    error "Không tìm thấy .so file nào trong $INSTALL_DIR/lib. Build có thể bị lỗi."
fi

info "Đã copy $SO_COUNT .so file vào $RUNTIME_DIR"

# ── Bước 5: Copy JAR vào project ─────────────────────────────────────────────
info "=== Bước 5: Copy coccoc-tokenizer.jar vào project ==="

JAR_SRC=$(find "$INSTALL_DIR" -name "coccoc-tokenizer.jar" 2>/dev/null | head -1)

if [ -z "$JAR_SRC" ]; then
    # Thử tìm trong build dir
    JAR_SRC=$(find "$BUILD_DIR" -name "*.jar" 2>/dev/null | head -1)
fi

if [ -z "$JAR_SRC" ]; then
    error "Không tìm thấy coccoc-tokenizer.jar sau khi build. Kiểm tra lại bước cmake -DBUILD_JAVA=1."
fi

mkdir -p "$(dirname "$JAR_DEST")"
cp "$JAR_SRC" "$JAR_DEST"
info "Copied: $JAR_SRC → $JAR_DEST"

# ── Bước 6: Kiểm tra dict ────────────────────────────────────────────────────
info "=== Bước 6: Kiểm tra tokenizer dictionary ==="

DICT_DIR="$INSTALL_DIR/share/tokenizer/dicts"
if [ ! -d "$DICT_DIR" ]; then
    error "Không tìm thấy dict tại $DICT_DIR. Kiểm tra lại quá trình build."
fi

DICT_COUNT=$(ls "$DICT_DIR" 2>/dev/null | wc -l)
info "Dictionary OK — $DICT_COUNT file tại $DICT_DIR"

# ── Bước 7: Kiểm tra nhanh tokenizer CLI ─────────────────────────────────────
info "=== Bước 7: Smoke test ==="

TOKENIZER_BIN=$(find "$INSTALL_DIR/bin" -name "tokenizer" 2>/dev/null | head -1)
if [ -n "$TOKENIZER_BIN" ]; then
    RESULT=$(echo "học phí sinh viên" | "$TOKENIZER_BIN" 2>/dev/null || true)
    if [ -n "$RESULT" ]; then
        info "Smoke test OK: 'học phí sinh viên' → $RESULT"
    else
        warn "Smoke test không trả về kết quả — kiểm tra thủ công bằng: echo 'test' | $TOKENIZER_BIN"
    fi
else
    warn "Không tìm thấy tokenizer binary để smoke test — bỏ qua."
fi

# ── Tóm tắt ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}=== CocCoc Tokenizer đã sẵn sàng! ===${NC}"
echo ""
echo "  Source    : $REPO_DIR"
echo "  Install   : $INSTALL_DIR"
echo "  Runtime   : $RUNTIME_DIR         (mount → /app/libs trong Docker)"
echo "  Dicts     : $DICT_DIR"
echo "  JAR       : $JAR_DEST            (dùng bởi Maven khi build Spring Boot)"
echo ""
echo "  docker-compose.yml đã cấu hình đúng volumes:"
echo "    $RUNTIME_DIR → /app/libs"
echo "    $DICT_DIR    → /usr/share/tokenizer/dicts"
echo "    $DICT_DIR    → /usr/local/share/tokenizer/dicts"
echo ""
echo "  Bước tiếp theo:"
echo "    docker compose build api"
echo "    docker compose up -d"
echo ""

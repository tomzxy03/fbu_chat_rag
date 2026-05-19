FROM python:3.13-slim
WORKDIR /app

# Ngăn debconf đưa ra cảnh báo về TTY và ép sử dụng IPv4 cho nhanh
ENV DEBIAN_FRONTEND=noninteractive
ENV PIP_DEFAULT_TIMEOUT=1000

# Thêm curl (healthcheck), tesseract (OCR ảnh), Vietnamese language pack
RUN apt-get update && apt-get install -y \
    libpq-dev \
    gcc \
    g++ \
    curl \
    tesseract-ocr \
    tesseract-ocr-vie \
    poppler-utils \
    ocrmypdf \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip setuptools wheel
# Sử dụng mirror để tải nhanh hơn nếu cần, hoặc giữ nguyên như bạn đã làm
RUN pip install --no-cache-dir --prefer-binary -r requirements.txt

COPY . .

# Đảm bảo port khớp với docker-compose
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]

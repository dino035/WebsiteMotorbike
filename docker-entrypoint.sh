#!/bin/sh
# Chạy mỗi lần container khởi động:
# 1. Nạp cấu hình bí mật (không nằm trong Git) vào ứng dụng.
# 2. Cho Tomcat nghe đúng cổng mà nền tảng yêu cầu (biến PORT).
# 3. Chạy Tomcat.

set -e

CLASSES=/usr/local/tomcat/webapps/ROOT/WEB-INF/classes
# Render gắn "Secret Files" vào /etc/secrets; có thể đổi thư mục bằng biến SECRETS_DIR khi chạy ở nơi khác
SECRETS="${SECRETS_DIR:-/etc/secrets}"

copy_secret() {
    if [ -f "$SECRETS/$1" ]; then
        mkdir -p "$(dirname "$CLASSES/$2")"
        cp "$SECRETS/$1" "$CLASSES/$2"
        echo "[deploy] da nap $1"
    else
        echo "[deploy] khong co $1 (bo qua)"
    fi
}

# Bắt buộc: thông tin kết nối cơ sở dữ liệu
copy_secret persistence.xml META-INF/persistence.xml
# Tùy chọn: thanh toán VNPay và gửi email quên mật khẩu (không có thì VNPay bị khóa, email in ra log)
copy_secret vnpay.properties vnpay.properties
copy_secret mail.properties mail.properties

if [ ! -f "$CLASSES/META-INF/persistence.xml" ]; then
    echo "[deploy] LOI: thieu persistence.xml. Hay tao Secret File ten persistence.xml (xem huong dan deploy)." >&2
    exit 1
fi

# Tomcat nghe đúng cổng nền tảng yêu cầu (Render đặt PORT; mặc định 8080 khi chạy Docker thường)
PORT="${PORT:-8080}"
sed -i "s/<Connector port=\"[0-9]*\" protocol=\"HTTP\/1.1\"/<Connector port=\"${PORT}\" protocol=\"HTTP\/1.1\"/" /usr/local/tomcat/conf/server.xml

exec catalina.sh run

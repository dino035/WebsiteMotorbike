# Đóng gói website thành một ảnh Docker để chạy trên Render (hoặc bất kỳ nơi nào chạy được Docker).
# Ảnh KHÔNG chứa mật khẩu: persistence.xml, vnpay.properties, mail.properties nằm ngoài Git
# và được nạp lúc khởi động từ thư mục bí mật (xem docker-entrypoint.sh và HUONG-DAN-DEPLOY trong docs).

# ---- Giai đoạn 1: biên dịch ra file WAR ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Tải thư viện trước để lớp này được cache khi chỉ sửa code
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Giai đoạn 2: chạy trên Tomcat 10.1 ----
FROM tomcat:10.1-jdk21

# Xóa các ứng dụng mẫu của Tomcat (docs, manager, examples...) để giảm bề mặt tấn công
RUN rm -rf /usr/local/tomcat/webapps/*

# Giải nén WAR thành ứng dụng gốc (đường dẫn /) để lúc khởi động còn chép được cấu hình bí mật vào
COPY --from=build /app/target/*.war /tmp/app.war
RUN mkdir -p /usr/local/tomcat/webapps/ROOT \
    && cd /usr/local/tomcat/webapps/ROOT \
    && jar xf /tmp/app.war \
    && rm /tmp/app.war

# Tắt cổng shutdown của Tomcat (không cần trong container) và thêm RemoteIpValve để Tomcat biết yêu cầu
# gốc dùng HTTPS và biết địa chỉ IP thật của khách khi đứng sau proxy của Render (X-Forwarded-Proto, X-Forwarded-For)
RUN sed -i 's/<Server port="8005"/<Server port="-1"/' /usr/local/tomcat/conf/server.xml \
    && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" />\n      </Host>#' /usr/local/tomcat/conf/server.xml

# Giờ Việt Nam cho toàn bộ ứng dụng (giờ đặt hàng, báo cáo theo ngày) và giới hạn bộ nhớ theo container
# (gói miễn phí của Render chỉ có 512 MB)
ENV CATALINA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh -XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Djava.security.egd=file:/dev/urandom"

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh && chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080
CMD ["docker-entrypoint.sh"]

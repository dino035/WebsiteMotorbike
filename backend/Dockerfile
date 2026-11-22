# Đóng gói backend Spring Boot thành ảnh Docker để chạy trên Render.
# KHÔNG chứa bí mật nào: mọi khóa và mật khẩu nạp lúc chạy qua biến môi trường (xem application.yml).
# Dockerfile ở gốc repo là của bản JSP chạy trên Tomcat — hai file độc lập, đừng lẫn.

# ---- Giai đoạn 1: biên dịch ra jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Tải thư viện trước để lớp này được dùng lại khi chỉ sửa code
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Giai đoạn 2: chỉ chạy, không cần Maven ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# spring-boot-maven-plugin đổi tên jar gốc thành *.jar.original, nên *.jar chỉ khớp đúng jar chạy được
COPY --from=build /app/target/*.jar app.jar

# Gói miễn phí của Render có 512MB: giới hạn heap theo tỉ lệ bộ nhớ thật, tránh bị hệ điều hành giết
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Render tự đặt biến PORT và application.yml đọc ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

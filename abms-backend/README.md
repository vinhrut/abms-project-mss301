# ABMS Backend

Backend microservice cho hệ thống quản lý chung cư, sử dụng Java 17, Spring Boot, Maven, PostgreSQL và Docker Compose.

## Các service

| Service | Port |
| --- | ---: |
| API Gateway | 8080 |
| Auth Service | 8081 |
| Apartment Service | 8082 |
| Vehicle Service | 8083 |
| Notification Service | 8084 |
| Report Service | 8085 |
| PostgreSQL | 5433 |

## Build bằng Maven

Yêu cầu Java 17 và Maven. Từ thư mục `abms-backend`:

```bash
mvn clean package -DskipTests
```

Nếu máy chưa cài Maven, có thể dùng Maven Wrapper nằm trong `report-service`:

```powershell
./report-service/mvnw.cmd -f pom.xml clean package -DskipTests
```

## Build và chạy bằng Docker

Tùy chọn: sao chép `.env.example` thành `.env` và đổi thông tin đăng nhập trước khi chạy.

```bash
docker compose build
docker compose up -d
```

Theo dõi trạng thái và log:

```bash
docker compose ps
docker compose logs -f
```

Dừng hệ thống nhưng giữ dữ liệu PostgreSQL:

```bash
docker compose down
```

Xóa cả dữ liệu PostgreSQL:

```bash
docker compose down -v
```

Frontend nên gọi các API thông qua Gateway tại `http://localhost:8080`.

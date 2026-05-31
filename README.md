# jwtSecurity
# Excel Upload API — Spring Boot + JWT + H2 Database + java17 + Gradle8.5 + Spring Boot version 3.2.2

A production-ready REST API for uploading and parsing Excel files, secured with JWT tokens that expire in **30 minutes**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Excel Parsing | Apache POI 5.2 |
| Persistence | Spring Data JPA + H2 (swap for MySQL/Postgres) |
| Java | 17+ |

---

## Project Structure

```
src/main/java/com/api/
├── config/
│   ├── DataInitializer.java       # Seeds default users on startup
│   ├── GlobalExceptionHandler.java
│   └── SecurityConfig.java        # JWT filter chain, CORS, STATELESS sessions
├── controller/
│   ├── AuthController.java        # /api/auth/login  /api/auth/register
│   └── ExcelController.java       # /api/excel/upload  /history  /{id}
├── dto/
│   ├── AuthDto.java               # LoginRequest, RegisterRequest, JwtResponse
│   └── ExcelUploadResponse.java
├── model/
│   ├── ExcelUpload.java           # JPA entity — upload metadata
│   └── User.java                  # JPA entity — user + roles
├── repository/
│   ├── ExcelUploadRepository.java
│   └── UserRepository.java
├── security/
│   ├── AuthEntryPointJwt.java     # 401 JSON response for unauthenticated calls
│   ├── AuthTokenFilter.java       # Reads & validates Bearer token per request
│   ├── JwtUtils.java              # Generate / validate / parse JWT (30 min expiry)
│   └── UserDetailsServiceImpl.java
└── service/
    └── ExcelService.java          # File validation, save to disk, POI parsing
```

---

## Quick Start

### 1. Build & Run

```bash
cd excel-upload-api
gradle build
```

The server starts on **http://localhost:8080**.  
An H2 console is available at **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:exceldb`).

Two users are seeded automatically:

| Username | Password | Roles |
|---|---|---|
| `admin` | `admin123` | USER, ADMIN |
| `user` | `user123` | USER |

---

## API Endpoints

### Auth

#### Register a new user
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@example.com",
  "password": "secret99"
}
```

**Response 200:**
```json
{
  "message": "User registered successfully.",
  "username": "alice"
}
```

---

#### Login → get JWT
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGci...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["USER", "ADMIN"],
  "expiresAt": "2024-05-15T10:30:00"
}
```

> The token is valid for **30 minutes** from the time of issue.

---

### Excel Upload (requires JWT)

All requests below need the header:
```
Authorization: Bearer <token>
```

#### Upload an Excel file
```
POST /api/excel/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=@report.xlsx
```

**Response 200:**
```json
{
  "uploadId": 1,
  "originalFileName": "report.xlsx",
  "fileSizeBytes": 12345,
  "totalSheets": 2,
  "totalRows": 150,
  "uploadedBy": "admin",
  "uploadedAt": "2024-05-15T10:05:00",
  "status": "SUCCESS",
  "message": "File uploaded and parsed successfully.",
  "headers": ["Name", "Age", "Email", "Score"],
  "previewRows": [
    { "Name": "Alice", "Age": 30, "Email": "a@b.com", "Score": 95.5 }
  ],
  "previewRowCount": 1
}
```

---

#### View upload history
```
GET /api/excel/history
Authorization: Bearer <token>
```

#### Get upload by ID
```
GET /api/excel/{id}
Authorization: Bearer <token>
```

---

## JWT Configuration

In `application.properties`:

```properties
# 256-bit Base64-encoded secret
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# 30 minutes = 1 800 000 milliseconds
jwt.expiration-ms=1800000
```

To change expiry to 1 hour: set `jwt.expiration-ms=3600000`.

---

## cURL Examples

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

# 2. Upload Excel
curl -X POST http://localhost:8080/api/excel/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/data.xlsx"

# 3. History
curl http://localhost:8080/api/excel/history \
  -H "Authorization: Bearer $TOKEN"
```

---

## Switching to MySQL / PostgreSQL

Replace the H2 dependency in `pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

Update `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/exceldb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

---

## File Upload Limits

| Setting | Default |
|---|---|
| Max file size | 20 MB |
| Max request size | 25 MB |
| Accepted formats | `.xlsx`, `.xls` |

---

## Security Notes

- JWT secret should be a strong random 256-bit key in production (use environment variables).
- Sessions are fully **stateless** — no HttpSession is created.
- Replace H2 with a persistent database for production.
- Store uploaded files in cloud storage (S3, GCS) instead of local disk in production.

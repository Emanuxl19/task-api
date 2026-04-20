# Task Management API

A RESTful API for task management built with Java 21 and Spring Boot 3.
This is a portfolio project focused on modern Java practices and professional-grade testing.

## Tech Stack

- **Java 21** — Records, pattern matching, virtual threads
- **Spring Boot 3.2** — Web, Data JPA, Validation
- **PostgreSQL** — Production database
- **Testcontainers** — Integration tests against a real PostgreSQL instance
- **JUnit 5 + Mockito** — Unit tests
- **SpringDoc OpenAPI** — Auto-generated Swagger documentation
- **Docker + Docker Compose** — Containerized setup

## Features

- Full CRUD for users and tasks
- Filtering by status and priority
- Pagination and sorting
- Overdue task detection
- Global error handling with consistent error responses
- Input validation
- Interactive API docs (Swagger UI)

## Quick Start

### Option 1 — H2 in-memory (no Docker needed)
```bash
git clone https://github.com/your-username/task-api
cd task-api
./mvnw spring-boot:run
```

Open http://localhost:8080/swagger-ui.html

### Option 2 — PostgreSQL with Docker Compose
```bash
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Option 3 — Full stack (app + database)
```bash
docker-compose up -d
```

## API Endpoints

| Method | Endpoint                              | Description                  |
|--------|---------------------------------------|------------------------------|
| GET    | /api/v1/users                         | List all users               |
| GET    | /api/v1/users/{id}                    | Get user by ID               |
| POST   | /api/v1/users                         | Create user                  |
| PUT    | /api/v1/users/{id}                    | Update user                  |
| DELETE | /api/v1/users/{id}                    | Delete user                  |
| GET    | /api/v1/users/{userId}/tasks          | List tasks (filterable)      |
| GET    | /api/v1/users/{userId}/tasks/overdue  | List overdue tasks           |
| GET    | /api/v1/tasks/{id}                    | Get task by ID               |
| POST   | /api/v1/users/{userId}/tasks          | Create task                  |
| PUT    | /api/v1/tasks/{id}                    | Update task                  |
| DELETE | /api/v1/tasks/{id}                    | Delete task                  |

### Filter and paginate tasks
```
GET /api/v1/users/1/tasks?status=TODO&page=0&size=10&sort=dueDate,asc
GET /api/v1/users/1/tasks?priority=HIGH
```

## Running Tests

```bash
# Unit tests only (no Docker required)
./mvnw test -Dgroups="unit"

# All tests including integration (requires Docker)
./mvnw test

# With coverage report
./mvnw verify
# Report at: target/site/jacoco/index.html
```

## Project Structure

```
src/
├── main/java/com/taskapi/
│   ├── config/          # OpenAPI configuration
│   ├── controller/      # REST controllers
│   ├── dto/             # Records for request/response (Java 21)
│   ├── entity/          # JPA entities
│   ├── exception/       # Custom exceptions + global handler
│   ├── repository/      # Spring Data JPA repositories
│   └── service/         # Business logic
└── test/java/com/taskapi/
    ├── controller/      # MockMvc tests (@WebMvcTest)
    ├── repository/      # Integration tests (Testcontainers)
    └── service/         # Unit tests (Mockito)
```

## Error Responses

All errors follow a consistent format:

```json
{
  "message": "User not found with id: 99",
  "status": 404,
  "timestamp": "2026-04-15T10:30:00"
}
```

## License

MIT

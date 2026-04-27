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
- JWT authentication with refresh token rotation
- Role-based authorization and row-level task access control
- OAuth2 login with Google and GitHub
- Request rate limiting for login, AI, and general API traffic
- Global error handling with consistent error responses
- Input validation
- Interactive API docs (Swagger UI)

## Quick Start

### Option 1 — H2 in-memory (no Docker needed)
```bash
git clone https://github.com/Emanuxl19/task-api.git
cd task-api
mvn spring-boot:run
```

Open http://localhost:8080/swagger-ui.html

### Option 2 — PostgreSQL with Docker Compose
```bash
docker-compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
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
# Full suite, including integration tests
mvn test

# Focus on one or more suites while developing
mvn -Dtest=TaskServiceTest,AuthServiceTest test

# With coverage report
mvn verify
# Report at: target/site/jacoco/index.html
```

Load and concurrency tests live in [load-tests/README.md](load-tests/README.md) and use `k6`.

## Development Workflow

- Contribution and release process: [CONTRIBUTING.md](CONTRIBUTING.md)
- Visual branch flow: [docs/workflow/git-workflow.md](docs/workflow/git-workflow.md)
- Complete GitFlow guide: [docs/workflow/gitflow-guide.md](docs/workflow/gitflow-guide.md)
- Load-testing guide: [load-tests/README.md](load-tests/README.md)
- Pull request template and CI live under [.github](.github)
- Kafka adoption notes: [docs/architecture/kafka.md](docs/architecture/kafka.md)

The CI workflow runs `mvn verify` on GitHub Actions with Java 21 for pull requests into `main` and `develop`.

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

# ♠️ Poker Settlement Management System

A full-stack, enterprise-grade financial settlement system for poker sessions. It manages user authentication, multi-player sessions, buy-ins, table invariants, and generates optimal peer-to-peer payout transactions using a **Minimum Cash-Flow algorithm**.

---

## 🛠️ Tech Stack

- **Backend**: Spring Boot 4.x, Java 21, Spring Security (Stateless JWT), Spring Data JPA, Hibernate, PostgreSQL
- **Frontend**: React 19, TypeScript, Vite, React Router, Custom Dark Luxury CSS
- **Database & Migrations**: PostgreSQL 16, Flyway Migrations (`V1__init_schema.sql`)
- **Reliability & Concurrency**: JPA Optimistic Locking (`@Version`), Pessimistic Write Locking (`SELECT FOR UPDATE`), Spring Boot Actuator (`/actuator/health`)
- **DevOps & Containers**: Docker multi-stage builds, Docker Compose, Nginx, GitHub Actions CI/CD

---

## 🚀 Quick Start (One Command via Docker Compose)

To spin up the entire application stack locally (PostgreSQL, Spring Boot backend, and React frontend):

```bash
docker compose up --build
```

- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080](http://localhost:8080)
- **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Swagger Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 💻 Running Locally Without Docker

### 1. Prerequisites
- Java 21+
- Node.js 20+
- PostgreSQL running on `localhost:5432` (or configure via `application-dev.properties`)

### 2. Run Backend
```bash
# Run automated tests (107 unit & integration tests including Flyway validation)
./mvnw test

# Start the Spring Boot application
./mvnw spring-boot:run
```

### 3. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173).

---

## ☁️ Production Deployment & CI/CD

For zero-cost production cloud hosting (Neon PostgreSQL + Render Backend + Vercel Frontend), consult the detailed guide:
👉 **[DEPLOYMENT.md](DEPLOYMENT.md)**

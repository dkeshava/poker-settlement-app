# 🚀 Poker Settlement System - Production Deployment Guide

This guide covers everything you need to know to take this application from local development to production.

---

## 🏗️ Architecture Overview

```mermaid
flowchart LR
    subgraph Client
        Browser["User Browser"]
    end

    subgraph FrontendHosting["Frontend (Vercel / Netlify / CDN)"]
        ReactApp["React 19 + Vite SPA\n(HTTPS)"]
    end

    subgraph BackendHosting["Backend (Render / Railway / Docker)"]
        SpringBoot["Spring Boot 4 / Java 21\nStateless Container\n/actuator/health"]
    end

    subgraph DatabaseHosting["Managed Database (Neon / Supabase)"]
        Postgres["PostgreSQL 16\nFlyway Migrations (V1)\nHikariCP Pool"]
    end

    Browser -->|HTTPS| ReactApp
    ReactApp -->|REST API + JWT| SpringBoot
    SpringBoot -->|JDBC Connection Pool| Postgres
```

---

## 🐳 1. Running Full-Stack Locally with Docker Compose

You can spin up the entire application stack (PostgreSQL + Spring Boot Backend + React Frontend + Nginx) with a single command:

```bash
docker compose up --build
```

### URLs:
- **Frontend App**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080](http://localhost:8080)
- **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Swagger API Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **PostgreSQL**: `localhost:5432` (`user: postgres`, `password: postgres`, `database: pokersettlement`)

To stop:
```bash
docker compose down
```
*(To wipe the local database volume and start fresh: `docker compose down -v`)*

---

## ☁️ 2. Free-Tier Cloud Production Deployment

Follow these 3 easy steps to deploy for **$0 / month**:

### Step 2.1: Create Managed PostgreSQL on Neon (Free)
1. Go to [Neon.tech](https://neon.tech) and sign up (free tier includes 0.5 GB storage, always-on branch).
2. Create a new project named `poker-settlement`.
3. In the Neon dashboard, copy your **Connection String**. It looks like:
   ```text
   postgresql://alex:AbCdEf123456@ep-cool-fog-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
4. Extract the following details:
   * **JDBC URL**:
     `jdbc:postgresql://ep-cool-fog-123456.us-east-2.aws.neon.tech/neondb?sslmode=require`
   * **Username**: `alex`
   * **Password**: `AbCdEf123456`

*(Flyway runs automatically on startup to create the `users`, `games`, `game_participants`, `buy_ins`, and `settlement_transactions` tables!)*

---

### Step 2.2: Deploy Backend to Render (Free Web Service)
1. Push your repository to GitHub.
2. Go to [Render.com](https://render.com) and create an account.
3. Click **New +** -> **Web Service** -> Connect your GitHub repo.
4. Settings:
   * **Name**: `poker-settlement-backend`
   * **Environment**: `Docker` (Render automatically detects your root `Dockerfile`)
   * **Branch**: `main`
   * **Instance Type**: `Free`
5. Under **Environment Variables**, add:
   | Key | Value | Description |
   |---|---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` | Activates production configuration |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<neon-host>/neondb?sslmode=require` | Neon database URL |
   | `SPRING_DATASOURCE_USERNAME` | `<neon-username>` | Database username |
   | `SPRING_DATASOURCE_PASSWORD` | `<neon-password>` | Database password |
   | `JWT_SECRET` | *(generate a 32+ character random secret string)* | e.g. `MySuperSecretProductionPokerJwtSigningKey2026!` |
   | `CORS_ALLOWED_ORIGINS` | `https://*.vercel.app,http://localhost:*` | Allowed frontend domains |
6. Click **Create Web Service**.
7. Once deployed, copy your backend URL:
   `https://poker-settlement-backend.onrender.com`
   Verify it is running:
   `https://poker-settlement-backend.onrender.com/actuator/health` (should return `{"status":"UP"}`)

---

### Step 2.3: Deploy Frontend to Vercel (Free)
1. Go to [Vercel.com](https://vercel.com) and sign in with GitHub.
2. Click **Add New...** -> **Project** -> Select your `poker-settlement` repo.
3. In the configuration screen:
   * **Root Directory**: Click edit and select `frontend`.
   * **Framework Preset**: `Vite` (auto-detected).
4. Under **Environment Variables**, add:
   | Key | Value |
   |---|---|
   | `VITE_API_BASE_URL` | `https://poker-settlement-backend.onrender.com` *(your Render backend URL)* |
5. Click **Deploy**.
6. Once deployed, Vercel gives you an instant HTTPS link (e.g. `https://poker-settlement.vercel.app`).
7. Update `CORS_ALLOWED_ORIGINS` on Render to include your exact Vercel URL:
   `CORS_ALLOWED_ORIGINS=https://poker-settlement.vercel.app,http://localhost:*`

---

## 🔒 3. Concurrency & Reliability Highlights

1. **JPA Optimistic Locking (`@Version`)**:
   - The `Game` entity has `@Version private Long version;`.
   - Prevents race conditions during simultaneous participant additions, buy-in edits, or round transitions without holding heavy database table locks.
   - Any concurrent collision returns HTTP 409 Conflict with an instructive message.
2. **Pessimistic Write Locking (`findByIdForUpdate`)**:
   - The financial settlement finalization route (`/games/{id}/settlement/finalize`) executes with `SELECT ... FOR UPDATE`.
   - Guarantees that only one manager transaction can finalize a game and write immutable peer-to-peer settlement transfers.
3. **Connection Pooling**:
   - HikariCP is pre-configured for serverless databases (connection timeout, max lifetime, idle cleanup).
4. **Structured Logging (SLF4J)**:
   - Configured via `logback-spring.xml`. All raw `System.out.println` calls have been replaced with timestamped, level-filtered SLF4J log statements.

---

## 🔄 4. Automated CI/CD Pipeline (GitHub Actions)

Your repository includes [`.github/workflows/ci.yml`](.github/workflows/ci.yml) which automatically executes on every push and pull request to `main`:
1. **Backend Job**: Sets up JDK 21, caches Maven dependencies, runs all **106 unit & integration tests**, and packages the JAR.
2. **Frontend Job**: Sets up Node 20, caches npm packages, runs TypeScript typecheck (`tsc -b`), and builds the production bundle (`vite build`).
3. **Docker Build Job**: Tests building both backend and frontend Docker containers to ensure cloud builds never fail.

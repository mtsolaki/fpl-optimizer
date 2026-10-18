# ⚽ FPL Optimizer Backend

[![Java](https://img.shields.io/badge/Java-21%2F25-orange.svg?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18%2F19-61DAFB.svg?logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-6.x-646CFF.svg?logo=vite)](https://vitejs.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg?logo=typescript)](https://www.typescriptlang.org/)
[![Nginx](https://img.shields.io/badge/Nginx-Alpine-009639.svg?logo=nginx)](https://nginx.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)](https://www.docker.com/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-grey.svg)](https://www.gnu.org/licenses/gpl-3.0)

> ⚠️ **Project Status: Active Development**  
> Core probabilistic projection engines, mathematical lineup optimization, multi-gameweek forecasting, automated batch reconciliation, and the interactive frontend dashboard are functional. Multi-gameweek transfer planning and chip optimization algorithms are actively being developed.

An advanced statistical modeling and optimization platform for **Fantasy Premier League (FPL)**. Built with **Spring Boot**, **PostgreSQL**, and **React (Vite/TypeScript)**, this system ingests live data from the official FPL API, runs granular probabilistic models (Poisson distribution, expected minutes, team strength degradation, fixture difficulty), and delivers optimal lineups, value picks, and model accuracy analytics via an integrated web dashboard.

---

## 📑 Table of Contents
- [Architecture & Engine Overview](#-architecture--engine-overview)
- [Key Features](#-key-features)
- [Project Structure](#-project-structure)
- [Quick Start (Docker)](#-quick-start-docker)
- [Environment Configuration](#-environment-configuration)
- [REST API Endpoints](#-rest-api-endpoints)
- [Local Development Setup](#-local-development-setup)
- [Running Tests](#-running-tests)
- [Roadmap](#-roadmap)
- [License](#-license)

---

## 🧠 Architecture & Engine Overview

The system is decoupled into statistical modeling, batch processing, and a high-performance presentation tier:

```text
[Official FPL API] 
       │ (FplApiClient / FplDataSyncService)
       ▼
 [PostgreSQL 16] (Player, Fixtures, Gameweek Logs, Market Info)
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                 Statistical Projection Engine               │
├─────────────────────────────────────────────────────────────┤
│ • PoissonMathUtils         → Goal, Assist & CS distributions│
│ • PlayerMinutesEngine      → Starting odds, rotation risk   │
│ • MatchStakesEngine        → Table context & match intensity│
│ • FixtureStrengthEngine    → Opponent-adjusted metrics      │
│ • LineupDegradationEngine  → Teammate injury penalties      │
│ • PlayerAttack/Defence     → xG, xA, xGC, clean sheets      │
│ • PlayerBonus/CardsEngine  → BPS projections & yellow risks │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                 Pipeline & Advisory Services                │
├─────────────────────────────────────────────────────────────┤
│ • UserLineupOptimizerService → Starting XI, bench, captain  │
│ • PlayerVfmService           → Value For Money (Pts/£M)     │
│ • GameweekPipelineScheduler  → Automated hourly sync & cron │
│ • GameweekProjectionBatch    → Historical backfill & 5-GW   │
│ • GameweekAnalyticsService   → Accuracy & evaluation metrics│
└─────────────────────────────────────────────────────────────┘
       │                                     │
       ▼ (REST API / JSON)                   ▼ (Reverse Proxy)
[Spring Boot Backend: 8080] ◄────── [Nginx & React UI: 3000]
```

---

## ✨ Key Features

* **Probabilistic Projections (`PoissonMathUtils`):** Calculates expected goal counts, clean sheet probabilities, and appearance points via Poisson distribution.
* **Squad Optimization (`UserLineupOptimizerService`):** Evaluates a manager's 15-man squad (`FplUserPicksResponse`) to select the optimal starting XI, bench order, and captaincy/vice-captaincy picks under official FPL formation rules.
* **Multi-Gameweek Forward Horizon (5 GWs):** Automatically projects upcoming player and team performance across a 5-gameweek window to power transfer and squad planning.
* **Historical Backfill & Automated Reconciliation:** Automatically identifies completed gameweeks lacking projections or settled scores and sequentially backfills outcomes upon application startup.
* **High-Performance In-Memory Batching:** Eliminates N+1 database queries through single-trip JPA repository fetching and in-memory Map lookups, completing batch gameweek projections in milliseconds.
* **Contextual Penalties (`LineupDegradationEngine`):** Adjusts player output when key creators or defensive anchors are injured or absent from their real-life team.
* **Value For Money (VFM) Metrics (`PlayerVfmService`):** Identifies high-performing budget enablers vs. overpriced assets across positions (`GKP`, `DEF`, `MID`, `FWD`).
* **Model Evaluation & Backtesting Dashboard:** Dedicated analytics endpoints and UI tables to compare projected xP against actual realized points.

---

## 📁 Project Structure

```text
fpl-optimizer/
├── docker-compose.yml       # Multi-container orchestration (DB, Backend, Frontend)
├── Dockerfile               # Backend multi-stage build (Temurin JDK -> JRE)
├── pom.xml                  # Backend Maven build configuration
├── .env.example             # Environment variable template
│
├── frontend/                # React Single Page Application (SPA)
│   ├── Dockerfile           # Frontend multi-stage build (Node -> Nginx Alpine)
│   ├── nginx.conf           # Reverse proxy routing (/api/ -> backend:8080)
│   ├── package.json         # React & UI dependencies
│   ├── vite.config.ts       # Vite build setup
│   └── src/                 # Components, hooks, types, and dashboard views
│
└── src/
    ├── main/java/com/fploptimizer/backend/
    │   ├── bootstrap/       # Startup hooks and seed listeners
    │   ├── client/          # FplApiClient: Integration with FPL REST endpoints
    │   ├── config/          # Spring beans, WebClient & async execution setup
    │   ├── controller/      # REST API exposure (Advisor, Analytics, Sync)
    │   ├── dto/             # Request & response transfer objects
    │   ├── entity/          # JPA entities (Player, Fixture, Gameweek, PlayerGameweekLog)
    │   │   └── player/      # Embedded metrics, outcomes, projections, and market info
    │   ├── exception/       # GlobalExceptionHandler for structured API errors
    │   ├── mapper/          # DTO/Entity mapping utilities (PlayerMapper)
    │   ├── repository/      # Spring Data JPA repositories with custom queries
    │   ├── scheduler/       # GameweekPipelineScheduler (cron batch syncing)
    │   └── service/
    │       ├── advisor/     # Lineup optimizer and VFM scoring services
    │       ├── projection/  # Mathematical engines (Attack, Defence, Minutes, Stakes)
    │       └── sync/        # Live outcome settlement, standings, and batching
    └── test/java/com/fploptimizer/backend/
        ├── controller/      # Controller integration tests
        └── service/         # Unit tests for Poisson math, VFM, and optimization
```
---

## 🚀 Quick Start (Docker)

Spin up the entire stack (PostgreSQL 16, Spring Boot Backend, and React/Nginx Frontend) with a single command:

### 1. Clone the repository
```bash
git clone https://github.com/mtsolaki/fpl-optimizer.git
cd fpl-optimizer
```

### 2. Configure environment
```bash
cp .env.example .env
```

### 3. Build & Run
```bash
docker compose up --build -d
```
### 4. Access Services
Frontend Web Dashboard: http://localhost:3000

Backend REST API: http://localhost:8080

PostgreSQL Database: localhost:5432

To follow container logs in real-time:
```bash 
docker compose logs -f backend
```
To stop all services:
```bash 
docker compose down 
```
---

## ⚙️ Environment Configuration

The following variables can be configured via `.env` or system environment variables:

| Variable | Default Value | Description |
|---|---|---|
| `DB_NAME` | `fpl_optimizer` | PostgreSQL database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `DB_PORT` | `5432` | Exposed database host port |
| `SPRING_PORT` | `8080` | Exposed Spring Boot host port |

---

## 🔌 REST API Endpoints

## 🔌 REST API Endpoints

### 1. Team Advisor & Lineup Optimizer
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/advisor/lineup/{teamId}?gameweek={gameweek}` | Recommends optimal XI, bench order, and captain/VC for an FPL Team ID |
| `GET` | `/api/v1/advisor/vfm?gameweek={gameweek}` | Retrieves Value-For-Money (VFM) rankings across all players and positions |

### 2. Model Analytics & Evaluation
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/analytics/gameweek?gameweek={gw}` | Retrieves all player projections alongside actual realized outcomes for model backtesting |

---

## 💻 Local Development Setup

If you prefer running the application outside of Docker:

### Prerequisites
* **Java 21 (JDK)**
* **Maven 3.9+** (or use the packaged `./mvnw`)
* **Node.js 20+** and **npm**
* **PostgreSQL 15+** running locally

### Steps
1. Create the database:
   ```sql
   CREATE DATABASE fpl_optimizer;
2. Verify credentials in:
   ```
    src/main/resources/application.properties
   ```

3. Build and launch:
   ```bash
   ./mvnw clean spring-boot:run
   ```
## 🧪 Running Tests

Run the test suite covering the optimization logic, VFM ranking, and controller endpoints:
```bash
   ./mvnw clean test
   ```
## Roadmap & Upcoming Features

- [x] **Core Statistical Engine:** Decoupled Poisson modeling, expected minutes, attack/defence adjustments, and degradation penalties.
- [x] **Advisory APIs:** User lineup recommendation, bench ordering, and Value-For-Money (VFM) endpoints.
- [x] **Automated Pipeline Lifecycle:** Automatic outcome reconciliation, historical backfill, and 5-gameweek forward projections.
- [x] **Performance Optimization:** N+1 query elimination via Map-based in-memory lookups and BuildKit dependency caching.
- [x] **Full-Stack Dockerization:** Multi-stage Docker builds for backend, frontend, and PostgreSQL with Nginx reverse proxy.
- [x] **Evaluation Dashboard:** Web interface and analytics endpoints to monitor and backtest model projections against real outcomes.
- [ ] **Multi-Gameweek Transfer Planner:** Dynamic programming algorithm to recommend optimal transfers across a 3–5 gameweek planning horizon under budget constraints.
- [ ] **FPL Chips Optimization Strategy:** Mathematical timing triggers for *Wildcard*, *Free Hit*, *Bench Boost*, and *Triple Captain*.
- [ ] **Mini-League & Competitor Tracker:** Head-to-head simulations and effective ownership (EO) risk-mitigation metrics.
- [ ] **Live Gameweek Rank Simulator:** Real-time auto-subs, provisional bonus calculation, and live rank forecasting.
## 📄 License

This project is licensed under the GNU AGPLv3 Licence - see the LICENSE file for details.

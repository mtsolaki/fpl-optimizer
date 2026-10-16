# ⚽ FPL Optimizer Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-grey.svg)](LICENSE)


> ⚠️ **Project Status: Active Development (Work in Progress)**  
> Core projection engines and lineup optimization services are implemented and functional. Real-time data pipeline enhancements, multi-gameweek transfer planning, and a dedicated frontend dashboard are currently under active construction.


An advanced statistical modeling and optimization backend engine for **Fantasy Premier League (FPL)**. Built with **Spring Boot** and **PostgreSQL**, this service ingests real-time data from the official FPL API, runs probabilistic distributions (Poisson modeling, expected minutes, lineup degradation, match stakes), and computes mathematical optimal lineups, captaincy choices, and value picks.

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

Instead of simple historical averages or static difficulty ratings, the optimizer decouples team-level and player-level performance into granular statistical engines:

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
│                 Decision & Advisory Services                │
├─────────────────────────────────────────────────────────────┤
│ • UserLineupOptimizerService → Starting XI, bench, captain  │
│ • PlayerVfmService           → Value For Money (Pts/£M)     │
│ • GameweekPipelineScheduler  → Automated batch processing   │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
[UserTeamAdvisorController] ──► REST API Clients / Frontend
```

---

## ✨ Key Features

* **Probabilistic Projections (`PoissonMathUtils`):** Calculates expected goal counts, clean sheet probabilities, and appearance points via Poisson distribution.
* **Squad Optimization (`UserLineupOptimizerService`):** Evaluates a manager's 15-man squad (`FplUserPicksResponse`) to select the optimal starting XI, bench order, and captaincy/vice-captaincy picks under official FPL formation rules.
* **Contextual Penalties (`LineupDegradationEngine`):** Adjusts player output when key creators or defensive anchors are injured or absent from their real-life team.
* **Value For Money (VFM) Metrics (`PlayerVfmService`):** Identifies high-performing budget enablers vs. overpriced assets across positions (`GKP`, `DEF`, `MID`, `FWD`).
* **Automated Batch Processing (`GameweekPipelineScheduler`):** Scheduled cron pipelines handle gameweek reconciliation, fixture projection generation, and live gameweek stats tracking.

---

## 📁 Project Structure

```text
fpl-optimizer/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── .env.example
└── src/
    ├── main/java/com/fploptimizer/backend/
    │   ├── Client/          # FplApiClient: Integration with FPL REST endpoints
    │   ├── config/          # Spring beans, WebClient & async execution setup
    │   ├── controller/      # REST API exposure (UserTeamAdvisorController)
    │   ├── dto/             # Request & response transfer objects (advisor, live, projection)
    │   ├── entity/          # JPA entities (Player, Fixture, Gameweek, PlayerGameweekLog)
    │   │   └── player/      # Embedded metrics, outcomes, projections, and market info
    │   ├── exception/       # GlobalExceptionHandler for structured API errors
    │   ├── mapper/          # DTO/Entity mapping utilities (PlayerMapper)
    │   ├── repository/      # Spring Data JPA repositories with custom queries
    │   ├── scheduler/       # GameweekPipelineScheduler (cron batch syncing)
    │   └── service/
    │       ├── advisor/     # Lineup optimizer and VFM scoring services
    │       ├── projection/  # Mathematical engines (Attack, Defence, Minutes, Stakes)
    │       └── sync/        # Live data fetching, reconciliation, and standings sync
    └── test/java/com/fploptimizer/backend/
        ├── controller/      # Controller integration tests
        └── service/         # Unit tests for Poisson math, VFM, and optimization
```
---

## 🚀 Quick Start (Docker)

Spin up the backend and a fully configured PostgreSQL instance with a single command:

### 1. Clone the repository
```bash
git clone [https://github.com/mtsolaki/fpl-optimizer.git](https://github.com/mtsolaki/fpl-optimizer.git)
cd fpl-optimizer
```

### 2. Configure environment
```bash
cp .env.example .env
docker compose up --build -d
```

### 3. Build & Run
```bash
docker compose up --build -d
```
API Base URL: http://localhost:8080

PostgreSQL: localhost:5432

To tail backend logs:
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

All team advice and projection capabilities are exposed under the advisor controller:

| Method | Endpoint                                              | Description |
|---|-------------------------------------------------------|---|
| `GET` | `/api/v1/advisor/lineup/{teamId}?gameweek={gameweek}` | Recommends optimal XI, bench order, and captain/VC for an FPL Team ID |
| `GET` | `/api/v1/advisor/vfm?gameweek={gameweek}`                                | Retrieves Value-For-Money (VFM) rankings across all players and positions |

---

## 💻 Local Development Setup

If you prefer running the application outside of Docker:

### Prerequisites
* **Java 21 (JDK)**
* **Maven 3.9+** (or use the packaged `./mvnw`)
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
- [x] **FPL API Ingestion:** Automatic sync of fixtures, bootstrap-static data, and gameweek logs.
- [x] **Dockerized Setup:** Complete multi-stage build and automated PostgreSQL container orchestration.
- [ ] **Multi-Gameweek Transfer Planner:** Dynamic programming algorithm to recommend optimal transfers across a 3–5 gameweek planning horizon under budget constraints.
- [ ] **FPL Chips Optimization Strategy:** Mathematical timing triggers for *Wildcard*, *Free Hit*, *Bench Boost*, and *Triple Captain*.
- [ ] **Mini-League & Competitor Tracker:** Head-to-head simulations and effective ownership (EO) risk-mitigation metrics.
- [ ] **Dedicated Web Dashboard (Frontend):** Interactive React/Next.js UI for lineup visualizer, fixture difficulty ticker, and player projection comparisons.
- [ ] **Live Gameweek Rank Simulator:** Real-time auto-subs, provisional bonus calculation, and live rank forecasting.
## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

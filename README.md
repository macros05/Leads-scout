# 🔍 Leads Scout

> Fullstack platform for automated local lead prospecting. Find businesses by sector, analyze their web performance with AI, detect their tech stack and manage the sales pipeline — all from a modern and secure interface.

---

## 📋 Table of Contents

- [Description](#description)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Environment Variables](#environment-variables)
- [Contributing](#contributing)
- [License](#license)

---

## 📖 Description

**Leads Scout** is a web application designed for sales and marketing teams to capture, classify and track leads (potential clients). It centralizes contact information, assigns pipeline statuses and allows browsing interaction history — with automated prospecting powered by Google Places API and Gemini AI.

---

## 🏗️ Architecture

```
Leads-scout/
├── clientes-scout/           # Backend — REST API (Java / Spring Boot)
└── clientes-scout-front/     # Frontend — SPA (Angular + TypeScript)
```

The architecture follows a **decoupled client–server** pattern: the frontend consumes the backend REST API over HTTP/JSON. Both layers can be deployed and scaled independently.

```
┌─────────────────────────┐      HTTP/REST + JWT      ┌──────────────────────────┐
│  Angular Frontend        │ ◄───────────────────────► │   Spring Boot API         │
│  (TypeScript)            │          JSON             │   (Java 17)               │
└─────────────────────────┘                            └──────────┬───────────────┘
                                                                   │
                                              ┌────────────────────┼────────────────────┐
                                              │                    │                    │
                                        ┌─────▼──────┐   ┌────────▼──────┐   ┌────────▼──────┐
                                        │  H2 / DB   │   │ Google Places │   │  Gemini AI    │
                                        └────────────┘   └───────────────┘   └───────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 4, Spring Security, JWT |
| Frontend | Angular 17+, TypeScript, TailwindCSS |
| Database | H2 (development) / PostgreSQL (production) |
| External APIs | Google Places API, Google PageSpeed API, Gemini AI |
| Security | BCrypt, JWT (jjwt 0.11.5), CORS |

---

## 📁 Project Structure

```
clientes-scout/
└── src/main/java/com/marcos/clientesscout/
    ├── controller/
    │   ├── AuthController.java       # Login and register
    │   └── ClientController.java     # Leads CRUD
    ├── model/
    │   ├── Client.java               # Main entity
    │   ├── User.java                 # System users
    │   └── ApplicationStatus.java    # Status enum
    ├── service/
    │   ├── ClientService.java        # Business logic + scouting
    │   ├── AnalyzerService.java      # PageSpeed + tech detection + AI
    │   ├── PlacesService.java        # Google Places API
    │   ├── JwtService.java           # Token generation and validation
    │   └── UserDetailsServiceImpl.java
    ├── security/
    │   ├── SecurityConfig.java       # Spring Security + CORS config
    │   └── JwtAuthFilter.java        # Per-request JWT filter
    ├── repository/
    ├── dto/
    └── exception/
        └── GlobalExceptionHandler.java

clientes-scout-front/
└── src/app/
    ├── auth/
    │   └── login.component.*         # Login / register screen
    ├── leads/
    │   ├── dashboard/                # Main panel
    │   ├── leads-list/               # Business list
    │   ├── scout-search/             # Search by sector
    │   └── audit-detail/             # Analysis detail
    ├── services/
    │   ├── auth.service.ts           # Login, register, token
    │   ├── auth.interceptor.ts       # Injects JWT into every request
    │   └── auth.guard.ts             # Protects private routes
    └── models/
```

---

## ✅ Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Node.js 18+** and **npm 9+**
- **Google Cloud API Key** with the following APIs enabled:
  - Places API
  - PageSpeed Insights API
- **Gemini API Key**

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/macros05/Leads-scout.git
cd Leads-scout
```

### 2. Configure the backend

Copy the configuration template:

```bash
cp clientes-scout/src/main/resources/application.properties.example \
   clientes-scout/src/main/resources/application.properties
```

Edit `application.properties` with your real values:

```properties
# Database
spring.datasource.url=jdbc:h2:file:~/data/clientesscout
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=your-secret-key-minimum-32-characters
jwt.expiration=86400000

# APIs
GOOGLE_API_KEY=your_google_api_key
google.api.key=${GOOGLE_API_KEY}
gemini.api.key=your_gemini_api_key
```

> ⚠️ This file is listed in `.gitignore` and will never be pushed to the repository.

### 3. Start the backend

```bash
cd clientes-scout
mvn spring-boot:run
```

API available at `http://localhost:8080`.

### 4. Start the frontend

```bash
cd clientes-scout-front
npm install
ng serve
```

App available at `http://localhost:4200`.

---

## 💻 Usage

1. Open the app in your browser and **sign in** or **register**.
2. Use **Scout Search** to find businesses by sector in your area.
3. The platform automatically analyzes each business website and generates AI-powered insights.
4. Manage and track your leads from the **Business List** and **Dashboard**.

---

## 📡 API Reference

### Authentication (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login — returns JWT token |

### Leads (requires token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/clients` | List all leads |
| GET | `/api/clients/{id}` | Get lead by ID |
| POST | `/api/clients` | Create a lead manually |
| PUT | `/api/clients/{id}` | Update a lead |
| DELETE | `/api/clients/{id}` | Delete a lead |
| GET | `/api/clients/budget/{amount}` | Filter by budget |
| POST | `/api/clients/scout?sector=` | Launch scouting by sector |
| GET | `/api/clients/test-speed?url=` | Analyze a URL's performance |

### Usage example

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@email.com","password":"password"}'

# Protected request
curl http://localhost:8080/api/clients \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 🔐 Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `jwt.secret` | JWT signing secret (min. 32 chars) | `my-super-secret-key-32chars!!` |
| `jwt.expiration` | Token expiration in milliseconds | `86400000` |
| `GOOGLE_API_KEY` | Google Cloud API key | `AIza...` |
| `gemini.api.key` | Gemini AI API key | `AIza...` |
| `spring.datasource.url` | Database connection URL | `jdbc:h2:file:~/data/leads` |
| `server.port` | Backend server port | `8080` |

> 🚨 **Never** include real credentials in source code or in the repository.

---

## 🔄 Scouting Flow

```
Scout Search (sector)
       │
       ▼
Google Places API → List of businesses
       │
       ▼
For each business:
  ├── With website → PageSpeed + Tech detection + Gemini AI issues
  └── No website  → Saved with score 0 as a direct opportunity
       │
       ▼
Saved to DB as lead with status INTERVIEW
```

---

## 🤝 Contributing

1. Fork the repository.
2. Create a branch: `git checkout -b feature/new-feature`
3. Commit your changes: `git commit -m 'feat: add new feature'`
4. Push the branch: `git push origin feature/new-feature`
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the **MIT** License. See the `LICENSE` file for details.

---

*Developed by [macros05](https://github.com/macros05)*

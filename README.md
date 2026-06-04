# Refill Me — Smart Fuel & Vehicle Management

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=node.js&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white)

A full-stack Android application for tracking vehicle fuel refills, maintenance services, trips, and personal expenses — with a cloud-synced backend and intelligent auto-calculation features.

---

## Features

### Fuel Tracking
- **Smart Auto-Calculation**: Enter any 2 of 3 values and the third is calculated automatically
  - `Total Price` + `Unit Price` → calculates Litres
  - `Total Price` + `Litres` → calculates Unit Price
  - `Unit Price` + `Litres` → calculates Total Price
- **Current Fuel Level Tracking**: Tracks fuel already in tank before each refill — not just new additions
- **Visual Fuel Gauge**: Color-coded indicators (Green >50%, Orange 25–50%, Red <25%)
- **Full Refill History**: Searchable log of every refill per vehicle

### Vehicle Management
- Add and manage multiple vehicles with name, model, license plate, and tank capacity
- Per-vehicle dashboard with fuel level and recent activity
- Vehicle photos via Unsplash integration

### Trip Logging
- Manual and automatic trip logging
- Trip details: start/end locations, distance, date
- Maps integration for route visualization
- All-trips history view

### Maintenance & Services
- Log service records (oil changes, tyre rotations, etc.)
- Service history per vehicle

### Personal Expenses
- Track non-fuel vehicle expenses (insurance, parking, fines, etc.)
- Categorized expense management
- Separate from fuel refill logs

### Authentication & Cloud Sync
- User accounts with secure signup/login
- JWT-based session management (30-day tokens)
- All data synced to Neon Postgres via Vercel serverless backend
- Local Room/SQLite database for offline-first access

---

## Architecture

### Android App — MVVM

```
app/src/main/java/com/simi/refillme/
├── data/
│   ├── entity/         # Room entities: Vehicle, Refill
│   ├── dao/            # Data Access Objects (Flow-based queries)
│   ├── database/       # RefillMeDatabase singleton
│   ├── repository/     # Repository layer abstracting Room + API
│   ├── auth/           # Ktor-based auth API client
│   ├── api/            # API service interfaces
│   └── preferences/    # DataStore + encrypted JWT storage
├── ui/
│   ├── screen/         # Composable screens
│   │   ├── auth/           # Login, Signup
│   │   ├── DashboardScreen.kt
│   │   ├── AddRefillScreen.kt
│   │   ├── RefillHistoryScreen.kt
│   │   ├── AddVehicleScreen.kt
│   │   ├── VehiclesScreen.kt
│   │   ├── AddServiceScreen.kt
│   │   ├── AddExpenseScreen.kt
│   │   ├── PersonalExpensesScreen.kt
│   │   ├── AddPersonalExpenseScreen.kt
│   │   ├── AllTripsScreen.kt
│   │   ├── AutoTripLoggingScreen.kt
│   │   ├── MapsScreen.kt
│   │   ├── AllLogsScreen.kt
│   │   └── SettingsScreen.kt
│   ├── components/     # Reusable UI components
│   ├── viewmodel/      # ViewModels with StateFlow
│   ├── theme/          # Material 3 theme
│   └── navigation/     # NavHost + route definitions
├── utils/
├── MainActivity.kt
├── RefillMeApplication.kt
└── ViewModelFactory.kt
```

**Data flow**: Room DAO (Flow) → Repository → ViewModel (StateFlow) → UI (collectAsState)

### Backend — Serverless (Vercel + Neon Postgres)

```
backend/
├── api/
│   ├── auth/
│   │   ├── login.js              # POST /api/auth/login
│   │   ├── signup.js             # POST /api/auth/signup
│   │   └── verify.js             # GET  /api/auth/verify
│   ├── middleware/               # JWT verification middleware
│   ├── vehicles.js               # GET/POST/PUT/DELETE /api/vehicles
│   ├── refills.js                # GET/POST/PUT/DELETE /api/refills
│   ├── trips.js                  # GET/POST/PUT/DELETE /api/trips
│   ├── services.js               # GET/POST /api/services
│   ├── expenses.js               # GET/POST /api/expenses
│   ├── personal-expenses.js      # GET/POST /api/personal-expenses
│   ├── personal-expense-categories.js
│   ├── specifications.js         # Vehicle specs
│   ├── search-photos.js          # Unsplash API proxy
│   ├── db.js                     # Neon Postgres connection pool
│   ├── init-db.js                # DB initialization endpoint
│   ├── init-tables.js            # Table creation
│   ├── init-trips-table.js
│   └── index.js                  # Health check: GET /api
└── vercel.json
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| Local DB | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| HTTP Client | Ktor 2.3.7 |
| Serialization | kotlinx.serialization |
| Secure Storage | EncryptedSharedPreferences |
| Navigation | Jetpack Navigation Compose |
| Backend Runtime | Node.js / Express |
| Serverless Platform | Vercel |
| Cloud Database | Neon Postgres |
| Auth | JWT (30-day) + bcrypt |
| Photos | Unsplash API |

---

## Database Schema

### Android — Room/SQLite

**vehicles**: `id`, `name`, `model`, `licensePlate`, `tankCapacity`, `currentFuelLevel`, `createdAt`, `isActive`

**refills**: `id`, `vehicleId` (FK → vehicles, cascade delete), `date`, `fuelBefore`, `refillAmount`, `fuelAfter`, `unitPrice`, `totalPrice`, `odometerReading?`, `notes?`, `location?`

### Backend — Neon Postgres

**users**: `id`, `email` (unique), `password_hash`, `full_name`, `created_at`

**vehicles**: `id`, `user_id` (FK), `name`, `model`, `license_plate`, `tank_capacity`, `current_fuel_level`, `created_at`, `is_active`

**refills**: `id`, `vehicle_id` (FK), `user_id` (FK), `date`, `fuel_before`, `refill_amount`, `fuel_after`, `unit_price`, `total_price`, `odometer_reading?`, `location?`, `notes?`

**trips**: `id`, `vehicle_id` (FK), `user_id` (FK), `start_location`, `end_location`, `distance`, `date`, ...

**services**: `id`, `vehicle_id` (FK), `user_id` (FK), `service_type`, `date`, `cost`, `notes?`

**expenses** / **personal_expenses**: expense tracking with categories

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (minSdk 24 — Android 7.0+)
- Node.js 18+ and npm
- Vercel CLI (`npm i -g vercel`)
- A [Neon](https://neon.tech) Postgres database

### Android App

```bash
# Clone the repo
git clone https://github.com/SsemakulaJonathan/Refill-Me.git
cd "Refill-Me"

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean assembleDebug
```

Open in Android Studio: **File → Open → select the project directory**, wait for Gradle sync.

### Backend API

```bash
cd backend
npm install

# Create your .env file
cp .env.example .env   # then fill in your credentials
```

**.env** (create this file — never commit it):
```env
PGHOST=your-neon-host.neon.tech
PGDATABASE=your-database-name
PGUSER=your-username
PGPASSWORD=your-password
PGSSLMODE=require
JWT_SECRET=your-64-char-hex-secret
```

Generate a JWT secret:
```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

```bash
# Run locally
npm run dev        # starts at http://localhost:3000

# Deploy to Vercel production
npm run deploy
```

Set all environment variables in your **Vercel project dashboard** under Settings → Environment Variables.

---

## API Reference

All authenticated endpoints require the header:
```
Authorization: Bearer <jwt_token>
```

### Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Create account |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/auth/verify` | Verify JWT token |

```bash
# Signup
curl -X POST https://your-app.vercel.app/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret","fullName":"Your Name"}'

# Login
curl -X POST https://your-app.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret"}'
```

### Resources (all require auth)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET/POST | `/api/vehicles` | List or create vehicles |
| PUT/DELETE | `/api/vehicles?id=:id` | Update or delete a vehicle |
| GET/POST | `/api/refills` | List or create refills |
| GET/POST | `/api/trips` | List or create trips |
| GET/POST | `/api/services` | List or create service records |
| GET/POST | `/api/expenses` | List or create expenses |
| GET/POST | `/api/personal-expenses` | Personal (non-vehicle) expenses |
| GET | `/api/personal-expense-categories` | Expense categories |
| GET | `/api/search-photos?query=:q` | Search vehicle photos (Unsplash) |

---

## Smart Calculation Logic

The signature feature — enter any 2 of 3 refill fields and the app calculates the third:

```
refillAmount = totalPrice / unitPrice
unitPrice    = totalPrice / refillAmount
totalPrice   = refillAmount × unitPrice
```

Implemented in `AddRefillScreen.kt` using `LaunchedEffect` watchers on each field.

**Example**: You paid $60 for 45.5 L → unit price auto-fills as **$1.32/L**

---

## Account Management

There is no in-app password reset. If you need to reset a password, generate a new bcrypt hash and update it directly in Neon:

```bash
# In the backend directory
node -e "const bcrypt = require('bcrypt'); bcrypt.hash('new-password', 10).then(h => console.log(h))"
```

Then run in the Neon SQL editor:
```sql
UPDATE users SET password_hash = '$2b$10$...' WHERE email = 'user@example.com';
```

---

## Project Structure (Top Level)

```
Refill-Me/
├── app/                    # Android application module
├── backend/                # Node.js/Vercel serverless API
├── gradle/                 # Gradle wrapper files
├── build.gradle.kts        # Root build config
├── settings.gradle.kts     # Module settings
├── gradle.properties
├── gradlew / gradlew.bat
├── fill_logo.png           # App logo
└── README.md
```

---

## Development Notes

- All DB operations are suspend functions running inside `viewModelScope`
- Manual DI via `ViewModelFactory` — no Dagger/Hilt
- Fuel level color thresholds: Green >50%, Orange 25–50%, Red <25%
- CORS is currently set to allow all origins — restrict for production
- Neon Postgres requires `PGSSLMODE=require`

---

## License

This project is for personal use. Feel free to fork and adapt it for your own needs.

## Developer

Built by **Jonathan Ssemakula** @ SIMI

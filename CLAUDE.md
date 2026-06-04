# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Refill Me is a fuel tracking application consisting of:
- **Android App**: Kotlin + Jetpack Compose mobile app with local SQLite storage
- **Backend API**: Node.js/Express serverless API deployed on Vercel with Neon Postgres

The app tracks vehicle fuel levels, refills, and expenses with intelligent auto-calculation features.

## Architecture

### Android App (MVVM Pattern)

The Android app follows a clean MVVM architecture:

```
app/src/main/java/com/simi/refillme/
├── data/
│   ├── entity/         # Room database entities (Vehicle, Refill)
│   ├── dao/            # Data Access Objects with Flow-based queries
│   ├── database/       # RefillMeDatabase singleton
│   ├── repository/     # Repository layer abstracting data sources
│   ├── auth/           # Authentication API service (Ktor client)
│   ├── api/            # API service interfaces
│   └── preferences/    # DataStore and encrypted preferences
├── ui/
│   ├── screen/         # Composable screens (dashboard, add refill, vehicles, auth)
│   ├── components/     # Reusable UI components
│   ├── viewmodel/      # ViewModels with StateFlow
│   ├── theme/          # Material 3 theme configuration
│   └── navigation/     # Navigation setup
├── utils/              # Utility functions
├── MainActivity.kt
├── RefillMeApplication.kt
└── ViewModelFactory.kt
```

**Data Flow**: Room DAO (Flow) → Repository (Flow) → ViewModel (StateFlow) → UI (collectAsState)

**Key Architectural Decisions**:
- Room database for local persistence with reactive Flow streams
- Manual dependency injection via ViewModelFactory (no Dagger/Hilt)
- Ktor HTTP client for REST API calls with kotlinx.serialization
- Encrypted SharedPreferences for secure JWT token storage
- Authentication endpoints consume backend API at `/api/auth/*`

### Backend API (Serverless)

The backend is structured as Vercel serverless functions:

```
backend/
├── api/
│   ├── auth/
│   │   ├── login.js      # POST /api/auth/login
│   │   ├── signup.js     # POST /api/auth/signup
│   │   └── verify.js     # GET /api/auth/verify
│   ├── vehicles.js       # Vehicle CRUD operations
│   ├── refills.js        # Refill CRUD operations
│   ├── search-photos.js  # Unsplash API integration
│   ├── init-db.js        # Database initialization
│   ├── init-tables.js    # Table creation endpoint
│   ├── db.js             # Postgres connection pool
│   └── index.js          # API health check
├── package.json
└── .env                  # Neon Postgres credentials + JWT secret
```

**Authentication**: JWT tokens (30-day expiry), bcrypt password hashing (10 rounds)
**Database**: Neon Postgres with connection pooling via `pg` module

## Development Commands

### Android App

#### Building and Running
```bash
# Navigate to project root
cd "/Users/jonathan/SIMI/Refill Me"

# Build debug APK
./gradlew assembleDebug

# Install debug APK on connected device
./gradlew installDebug

# Clean build
./gradlew clean
./gradlew assembleDebug

# Run tests
./gradlew test
```

#### Android Studio
- Open project in Android Studio Hedgehog (2023.1.1) or newer
- Ensure JDK 17 is selected (File → Project Structure → SDK Location → Gradle JDK)
- Required: Android SDK 34, minSdk 24, compileSdk 34
- Run: Click green ▶️ button or `Ctrl + R` (Mac: `Cmd + R`)

### Backend API

```bash
cd backend

# Install dependencies
npm install

# Local development (uses Vercel Dev)
npm run dev
# Server runs at http://localhost:3000

# Deploy to Vercel production
npm run deploy
```

#### Environment Variables
The backend requires these environment variables (configured in `.env` locally, set in Vercel dashboard for production):
- `PGHOST`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, `PGSSLMODE` (Neon Postgres)
- `JWT_SECRET` (Generate with: `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`)

## Smart Calculation Logic

The app's signature feature is auto-calculating the missing value when 2 of 3 fields are provided:
- `refillAmount = totalPrice / unitPrice`
- `unitPrice = totalPrice / refillAmount`
- `totalPrice = refillAmount × unitPrice`

This logic is implemented in the `AddRefillScreen` composable using `LaunchedEffect` to watch for field changes and trigger calculations.

## Database Schemas

### Android (Room/SQLite)

**vehicles**: id (PK), name, model, licensePlate, tankCapacity, currentFuelLevel, createdAt, isActive

**refills**: id (PK), vehicleId (FK), date, fuelBefore, refillAmount, fuelAfter, unitPrice, totalPrice, odometerReading, notes, location

Cascade delete: When a vehicle is deleted, all associated refills are deleted.

### Backend (Neon Postgres)

**users**: id (PK), email (unique), password_hash, full_name, created_at

**vehicles**: id (PK), user_id (FK), name, model, license_plate, tank_capacity, current_fuel_level, created_at, is_active

**refills**: id (PK), vehicle_id (FK), user_id (FK), date, fuel_before, refill_amount, fuel_after, unit_price, total_price, odometer_reading, location, notes

## Testing the App

### Manual Test Flow
1. Launch app → Dashboard displays with empty state
2. Tap FAB → Add vehicle (name, model, tank capacity)
3. Tap FAB again → Select vehicle → Add refill with any 2 of 3 values
4. Verify third value auto-calculates
5. Verify fuel level updates on dashboard with color-coded gauge

### API Testing (curl)
```bash
# Signup
curl -X POST http://localhost:3000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","fullName":"Test User"}'

# Login
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Verify token (replace TOKEN)
curl -X GET http://localhost:3000/api/auth/verify \
  -H "Authorization: Bearer TOKEN"
```

## Important Development Notes

### Android App
- The app uses Material 3 with custom gradient cards
- Fuel level indicators: Green (>50%), Orange (25-50%), Red (<25%)
- Navigation uses Jetpack Navigation Compose with NavHost
- All database operations are suspend functions running in viewModelScope
- The ViewModelFactory pattern is used to inject repositories into ViewModels

### Backend API
- All endpoints are serverless functions exported as `module.exports = (req, res) => {...}`
- CORS is configured to allow all origins (adjust for production)
- JWT tokens are required for authenticated endpoints (verify via middleware)
- Connection pooling is critical for Neon Postgres performance

## Key Dependencies

### Android
- Jetpack Compose (BOM 2023.10.01)
- Room 2.6.1 with KSP compiler
- Ktor Client 2.3.7 for HTTP
- kotlinx.serialization 1.6.2
- DataStore Preferences 1.0.0
- Security Crypto 1.1.0-alpha06 (encrypted storage)
- Navigation Compose 2.7.5

### Backend
- express ^4.18.2
- pg ^8.11.3 (PostgreSQL client)
- bcrypt ^5.1.1
- jsonwebtoken ^9.0.2
- cors ^2.8.5

## Troubleshooting

### Gradle Sync Failed
```bash
./gradlew clean
# In Android Studio: File → Invalidate Caches → Invalidate and Restart
```

### Backend Database Connection Issues
- Verify `.env` file exists with correct Neon Postgres credentials
- Ensure `PGSSLMODE=require` is set
- Check Neon dashboard for connection limits

### App Crashes on Launch
- Check Logcat for Room migration errors
- Verify all required permissions in AndroidManifest.xml
- Ensure database initialization in RefillMeApplication.kt

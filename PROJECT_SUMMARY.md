# Refill Me - Project Summary

## 📋 Project Overview

**Refill Me** is a modern Android fuel expense tracking application built with Kotlin and Jetpack Compose. It solves a major limitation of existing apps like "Simply Auto" by tracking the actual fuel level in your tank, not assuming it starts at zero.

## 🎯 Core Innovation

### The Problem with Simply Auto
- Assumes tank is at 0 litres when you refill
- No way to input existing fuel level
- Manual calculations required

### How Refill Me Solves It
- ✅ Tracks current fuel level in each vehicle
- ✅ Auto-fills "fuel before" when adding a refill
- ✅ Intelligent auto-calculation: enter any 2 of 3 values (litres, unit price, total price)
- ✅ Validates against tank capacity to prevent errors

## 🏗️ Technical Architecture

### Technology Stack
```
Language:        Kotlin 1.9.20
UI:             Jetpack Compose (Material 3)
Architecture:    MVVM (Model-View-ViewModel)
Database:        Room (SQLite) 2.6.1
Async:          Kotlin Coroutines + Flow
Navigation:      Navigation Compose 2.7.5
Min SDK:         24 (Android 7.0)
Target SDK:      34 (Android 14)
```

### Project Structure
```
Refill Me/
├── app/
│   ├── build.gradle.kts              # App-level dependencies
│   ├── proguard-rules.pro            # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml       # App manifest
│       ├── res/
│       │   ├── values/
│       │   │   ├── strings.xml       # String resources
│       │   │   └── themes.xml        # Theme configuration
│       │   └── xml/
│       │       ├── backup_rules.xml
│       │       └── data_extraction_rules.xml
│       └── java/com/simi/refillme/
│           ├── data/
│           │   ├── entity/
│           │   │   ├── Vehicle.kt              # Vehicle entity
│           │   │   ├── Refill.kt               # Refill entity
│           │   │   └── VehicleWithRefills.kt   # Relation entity
│           │   ├── dao/
│           │   │   ├── VehicleDao.kt           # Vehicle database operations
│           │   │   └── RefillDao.kt            # Refill database operations
│           │   ├── database/
│           │   │   └── RefillMeDatabase.kt     # Database configuration
│           │   └── repository/
│           │       ├── VehicleRepository.kt    # Vehicle data layer
│           │       └── RefillRepository.kt     # Refill data layer
│           ├── ui/
│           │   ├── screen/
│           │   │   ├── DashboardScreen.kt      # Main dashboard UI
│           │   │   ├── AddRefillScreen.kt      # Add refill form
│           │   │   └── VehiclesScreen.kt       # Vehicle list & add
│           │   ├── theme/
│           │   │   ├── Color.kt                # Color palette
│           │   │   ├── Type.kt                 # Typography
│           │   │   └── Theme.kt                # Theme configuration
│           │   ├── viewmodel/
│           │   │   ├── VehicleViewModel.kt     # Vehicle business logic
│           │   │   └── RefillViewModel.kt      # Refill business logic
│           │   └── navigation/
│           │       ├── Screen.kt               # Screen routes
│           │       └── AppNavigation.kt        # Navigation setup
│           ├── MainActivity.kt                  # App entry point
│           ├── RefillMeApplication.kt          # Application class
│           └── ViewModelFactory.kt             # ViewModel factory
├── build.gradle.kts                 # Root build configuration
├── settings.gradle.kts              # Project settings
├── gradle.properties                # Gradle properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties    # Gradle wrapper config
├── .gitignore                       # Git ignore rules
├── README.md                        # Main documentation
└── QUICK_START.md                   # Quick start guide
```

## 🎨 UI Components

### Screens
1. **Dashboard Screen** (`DashboardScreen.kt`)
   - Overview of vehicles and refills
   - Statistics cards with gradients
   - Recent refills list
   - Navigation to other screens

2. **Add Refill Screen** (`AddRefillScreen.kt`)
   - Vehicle selection
   - Smart input fields with auto-calculation
   - Validation and error handling
   - Optional fields (odometer, location, notes)

3. **Vehicles Screen** (`VehiclesScreen.kt`)
   - List of all vehicles
   - Detailed vehicle cards with fuel gauges
   - Add vehicle dialog
   - Visual fuel level indicators

### Reusable Components
- `StatCard`: Gradient stat cards with icons
- `VehicleCard`: Vehicle display with fuel gauge
- `RefillCard`: Refill entry display
- `InfoChip`: Small info badges
- `EmptyStateCard`: Empty state placeholders

## 🗄️ Database Schema

### Tables

#### vehicles
```sql
id                  LONG PRIMARY KEY AUTOINCREMENT
name                TEXT NOT NULL
model               TEXT NOT NULL
licensePlate        TEXT NOT NULL
tankCapacity        REAL NOT NULL
currentFuelLevel    REAL NOT NULL DEFAULT 0.0
createdAt           INTEGER NOT NULL
isActive            INTEGER NOT NULL DEFAULT 1
```

#### refills
```sql
id                  LONG PRIMARY KEY AUTOINCREMENT
vehicleId           LONG NOT NULL (FOREIGN KEY -> vehicles.id)
date                INTEGER NOT NULL
fuelBefore          REAL NOT NULL
refillAmount        REAL NOT NULL
fuelAfter           REAL NOT NULL
unitPrice           REAL NOT NULL
totalPrice          REAL NOT NULL
odometerReading     REAL NULL
notes               TEXT NULL
location            TEXT NULL
```

### Relationships
- One Vehicle → Many Refills (One-to-Many)
- Foreign key with CASCADE delete

## 🔄 Data Flow

### Adding a Refill
```
User Input
    ↓
RefillViewModel
    ↓
Validation & Auto-calculation
    ↓
RefillRepository
    ↓
RefillDao
    ↓
Room Database
    ↓
Update Vehicle Fuel Level
    ↓
VehicleRepository
    ↓
VehicleDao
    ↓
UI Update via Flow
```

## 🧮 Smart Calculation Logic

The app implements intelligent field calculation in `RefillViewModel.kt`:

```kotlin
// Calculation priority:
1. If amount is missing: amount = totalPrice / unitPrice
2. If unitPrice is missing: unitPrice = totalPrice / amount
3. If totalPrice is missing: totalPrice = amount * unitPrice

// Validation:
- Prevents division by zero
- Validates numeric inputs
- Checks tank capacity limits
- Ensures fuel after refill ≤ tank capacity
```

## 🎨 Design System

### Color Palette
- **Primary**: Blue (#2196F3) - Main actions, app bar
- **Secondary**: Teal (#03DAC6) - FAB, accents
- **Success**: Green (#4CAF50) - High fuel levels
- **Warning**: Orange (#FF9800) - Medium fuel levels
- **Error**: Red (#F44336) - Low fuel levels, errors
- **Background**: Light Gray (#F5F5F5)
- **Surface**: White (#FFFFFF)

### Typography
- Material 3 default typography scale
- Bold headers (headlineSmall, titleLarge)
- Medium body text (bodyLarge, bodyMedium)
- Small labels (bodySmall, labelSmall)

### Spacing
- Cards: 16dp padding
- Between items: 12-16dp
- Screen padding: 16dp
- Card corner radius: 12-16dp

## 📦 Dependencies

### Core
- androidx.core:core-ktx:1.12.0
- androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
- androidx.activity:activity-compose:1.8.1

### Compose
- compose-bom:2023.10.01
- androidx.compose.material3
- androidx.compose.material:material-icons-extended

### Navigation
- androidx.navigation:navigation-compose:2.7.5

### Database
- androidx.room:room-runtime:2.6.1
- androidx.room:room-ktx:2.6.1
- KSP for Room compiler

### Coroutines
- org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3

## 🚀 Build & Run

### Requirements
- Android Studio Hedgehog (2023.1.1)+
- JDK 17
- Android SDK 34
- Gradle 8.2

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

## 🔐 Data Privacy
- All data stored locally in SQLite
- No internet connection required
- No data sent to external servers
- User has full control over their data

## 🔮 Future Features Roadmap

### Phase 2: Analytics
- [ ] Fuel consumption statistics
- [ ] Monthly expense charts
- [ ] Average price per litre trends
- [ ] Fuel efficiency (km/L or MPG)

### Phase 3: Enhancements
- [ ] Dark mode support
- [ ] Export data (CSV/PDF)
- [ ] Backup & restore
- [ ] Multiple currency support
- [ ] Maintenance reminders

### Phase 4: Advanced
- [ ] Trip tracking
- [ ] Fuel station price comparisons
- [ ] Cost splitting for shared vehicles
- [ ] Widget for quick refill entry

## 📝 Code Quality

### Best Practices Implemented
- ✅ MVVM architecture for separation of concerns
- ✅ Repository pattern for data abstraction
- ✅ Kotlin Coroutines for async operations
- ✅ Flow for reactive data streams
- ✅ Room for type-safe database access
- ✅ Compose for declarative UI
- ✅ Material Design 3 guidelines
- ✅ Single source of truth for data
- ✅ Lifecycle-aware components

### Code Style
- Kotlin naming conventions
- Clear function and variable names
- Documented complex logic
- Modular, reusable components

## 🎯 Key Achievements

1. **Solves Real Problem**: Addresses the limitation of existing apps
2. **Smart Calculations**: Reduces user effort with auto-calculations
3. **Beautiful UI**: Modern, intuitive design
4. **Robust Architecture**: Scalable and maintainable codebase
5. **Type Safety**: Kotlin + Room for compile-time safety
6. **Offline First**: Works without internet connection

---

**Built with ❤️ for better fuel expense tracking**

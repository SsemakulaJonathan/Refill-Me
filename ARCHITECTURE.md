# Refill Me - App Flow & Architecture Diagrams

## 📱 User Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      APP LAUNCH                             │
│                          ↓                                  │
│                   Dashboard Screen                          │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  • Statistics Cards (Vehicles Count, Refills Count) │  │
│  │  • My Vehicles Section (with fuel gauges)           │  │
│  │  • Recent Refills Section                           │  │
│  │  • FAB (+) Button                                    │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                   ↓
   [Vehicles]       [Add Refill]         [History]
   
┌──────────────┐    ┌─────────────────┐    ┌──────────────┐
│   Vehicles   │    │   Add Refill    │    │   History    │
│   Screen     │    │    Screen       │    │   (Future)   │
├──────────────┤    ├─────────────────┤    ├──────────────┤
│ • List all   │    │ 1. Select       │    │ • All refills│
│   vehicles   │    │    vehicle      │    │   chronolog  │
│ • Fuel gauge │    │ 2. Fuel before  │    │ • Filter by  │
│ • Add new    │    │    (auto-fill)  │    │   vehicle    │
│   vehicle    │    │ 3. Enter 2 of 3:│    │ • Statistics │
│              │    │    - Litres     │    │              │
│              │    │    - Unit price │    │              │
│              │    │    - Total      │    │              │
│              │    │ 4. Optional:    │    │              │
│              │    │    - Odometer   │    │              │
│              │    │    - Location   │    │              │
│              │    │    - Notes      │    │              │
│              │    │ 5. Save         │    │              │
└──────────────┘    └─────────────────┘    └──────────────┘
```

## 🏗️ MVVM Architecture Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                          UI Layer                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Jetpack Compose Screens                      │   │
│  │  • DashboardScreen                                   │   │
│  │  • AddRefillScreen                                   │   │
│  │  • VehiclesScreen                                    │   │
│  └────────────────────┬─────────────────────────────────┘   │
└───────────────────────┼───────────────────────────────────────┘
                        │ observes State
                        ↓
┌───────────────────────────────────────────────────────────────┐
│                       ViewModel Layer                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  VehicleViewModel      │     RefillViewModel         │   │
│  │  • StateFlow<Vehicles> │     • StateFlow<Refills>    │   │
│  │  • CRUD operations     │     • Smart calculations    │   │
│  │  • Business logic      │     • Form state mgmt       │   │
│  └────────────────────┬───┴────────┬────────────────────┘   │
└───────────────────────┼────────────┼───────────────────────────┘
                        │            │
                        ↓            ↓
┌───────────────────────────────────────────────────────────────┐
│                     Repository Layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  VehicleRepository  │    RefillRepository            │   │
│  │  • Data abstraction │    • Data abstraction          │   │
│  │  • Flow<Data>       │    • Flow<Data>                │   │
│  └────────────────────┬────────────┬────────────────────┘   │
└───────────────────────┼────────────┼───────────────────────────┘
                        │            │
                        ↓            ↓
┌───────────────────────────────────────────────────────────────┐
│                      Data Layer (Room)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           RefillMeDatabase                           │   │
│  │  ┌─────────────────┐      ┌─────────────────┐      │   │
│  │  │   VehicleDao    │      │    RefillDao    │      │   │
│  │  │  • CRUD methods │      │  • CRUD methods │      │   │
│  │  │  • Queries      │      │  • Queries      │      │   │
│  │  │  • Flow data    │      │  • Flow data    │      │   │
│  │  └────────┬────────┘      └────────┬────────┘      │   │
│  └───────────┼──────────────────────────┼──────────────┘   │
└──────────────┼──────────────────────────┼────────────────────┘
               ↓                          ↓
┌───────────────────────────────────────────────────────────────┐
│                      SQLite Database                          │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  vehicles table  │←────────│  refills table   │          │
│  │  • id (PK)       │  FK     │  • id (PK)       │          │
│  │  • name          │         │  • vehicleId(FK) │          │
│  │  • model         │         │  • fuelBefore    │          │
│  │  • tankCapacity  │         │  • refillAmount  │          │
│  │  • currentFuel   │         │  • totalPrice    │          │
│  └──────────────────┘         └──────────────────┘          │
└───────────────────────────────────────────────────────────────┘
```

## 🔄 Add Refill Flow (Detailed)

```
User Opens Add Refill Screen
         ↓
    ┌────────────────────────────────┐
    │ Load vehicles from database    │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ User selects vehicle           │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ Auto-fill "fuelBefore" from    │
    │ vehicle.currentFuelLevel       │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ User enters 2 of 3 fields:     │
    │ • refillAmount                 │
    │ • unitPrice                    │
    │ • totalPrice                   │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ LaunchedEffect detects change  │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ Calculate missing field:       │
    │                                │
    │ IF missing refillAmount:       │
    │   refillAmount =               │
    │     totalPrice / unitPrice     │
    │                                │
    │ IF missing unitPrice:          │
    │   unitPrice =                  │
    │     totalPrice / refillAmount  │
    │                                │
    │ IF missing totalPrice:         │
    │   totalPrice =                 │
    │     refillAmount * unitPrice   │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ Calculate fuelAfter:           │
    │ fuelAfter =                    │
    │   fuelBefore + refillAmount    │
    └────────────┬───────────────────┘
                 ↓
    ┌────────────────────────────────┐
    │ Validate:                      │
    │ • fuelAfter ≤ tankCapacity    │
    │ • All values > 0              │
    │ • Vehicle selected            │
    └────────────┬───────────────────┘
                 ↓
         ┌───────┴────────┐
         │                │
    Valid?            Invalid
         │                │
         ↓                ↓
    ┌─────────┐    ┌──────────┐
    │  Save   │    │  Show    │
    │ Refill  │    │  Error   │
    └────┬────┘    └──────────┘
         ↓
    ┌────────────────────────────────┐
    │ 1. Insert refill to database   │
    │ 2. Update vehicle.currentFuel  │
    │    to fuelAfter                │
    │ 3. Navigate back to Dashboard  │
    └────────────────────────────────┘
```

## 🎨 UI Component Hierarchy

```
MainActivity
    └── RefillMeTheme
        └── Surface
            └── AppNavigation (NavHost)
                ├── DashboardScreen
                │   ├── Scaffold
                │   │   ├── TopAppBar
                │   │   ├── FloatingActionButton
                │   │   └── LazyColumn
                │   │       ├── StatCard (Vehicles)
                │   │       ├── StatCard (Refills)
                │   │       ├── VehicleCard (for each vehicle)
                │   │       │   ├── Icon
                │   │       │   ├── Text (name, model)
                │   │       │   └── LinearProgressIndicator
                │   │       └── RefillCard (for each refill)
                │   │           ├── Text (vehicle, date, price)
                │   │           └── InfoChip (litres, unit price)
                │   └── AddVehicleDialog (conditional)
                │
                ├── AddRefillScreen
                │   └── Scaffold
                │       ├── TopAppBar
                │       └── Column (scrollable)
                │           ├── VehicleSelectionCard
                │           ├── RefillDetailsCard
                │           │   ├── OutlinedTextField (fuelBefore)
                │           │   ├── OutlinedTextField (refillAmount)
                │           │   ├── OutlinedTextField (unitPrice)
                │           │   ├── OutlinedTextField (totalPrice)
                │           │   └── InfoCard (calculation hint)
                │           ├── OptionalDetailsCard
                │           │   ├── OutlinedTextField (odometer)
                │           │   ├── OutlinedTextField (location)
                │           │   └── OutlinedTextField (notes)
                │           └── Button (Save)
                │
                └── VehiclesScreen
                    └── Scaffold
                        ├── TopAppBar
                        ├── FloatingActionButton
                        └── LazyColumn / EmptyState
                            └── VehicleDetailCard (for each)
                                ├── Icon + Vehicle Info
                                ├── FuelLevelCard
                                │   ├── Text (fuel level)
                                │   └── LinearProgressIndicator
                                └── StatsRow
                                    ├── StatItemCard (tank capacity)
                                    └── StatItemCard (active status)
```

## 💾 Data Flow Pattern

```
┌──────────────────────────────────────────────────────────────┐
│                    Reactive Data Flow                        │
└──────────────────────────────────────────────────────────────┘

Room Database
     ↓
    DAO
     ↓ Flow<List<Entity>>
  Repository
     ↓ Flow<List<Entity>>
  ViewModel
     ↓ StateFlow<List<Entity>>
    UI (Compose)
     ↓ collectAsState()
  Recompose on data change


┌──────────────────────────────────────────────────────────────┐
│                   Write Operation Flow                       │
└──────────────────────────────────────────────────────────────┘

UI Event (Button Click)
     ↓
  ViewModel
     ↓ viewModelScope.launch
  Repository
     ↓ suspend function
    DAO
     ↓ @Insert / @Update
Room Database
     ↓ triggers Flow update
    UI updates automatically
```

## 🔢 Smart Calculation Logic Flow

```
┌─────────────────────────────────────────────────────────────┐
│         Smart Field Calculation Algorithm                   │
└─────────────────────────────────────────────────────────────┘

Input Fields Changed:
  • refillAmount
  • unitPrice  
  • totalPrice

        ↓
LaunchedEffect Triggered
        ↓
    Parse Values:
      amount = refillAmount?.toDouble()
      unit = unitPrice?.toDouble()
      total = totalPrice?.toDouble()
        ↓
    Count Non-Null Values
        ↓
   ┌────┴────┐
   │         │
0 or 1    2 values present
values        │
   │          ↓
   │     Determine Missing Field
   │          ↓
   │    ┌─────┼──────┐
   │    │     │      │
   │  amount unit  total
   │  missing missing missing
   │    │     │      │
   │    ↓     ↓      ↓
   │  calc  calc   calc
   │  from  from   from
   │  other other other
   │  two   two    two
   │    │     │      │
   │    └─────┼──────┘
   │          ↓
   │    Update Form State
   │    with calculated value
   │          ↓
   └───→ Display in UI

Formulas:
  refillAmount = totalPrice / unitPrice
  unitPrice = totalPrice / refillAmount
  totalPrice = refillAmount × unitPrice
```

## 🎨 Color-Coded Fuel Levels

```
Tank Capacity: 100%
├─────────────────────────────────────────────┤

🟢 GREEN (51% - 100%)
├──────────────────────────┤
│      Safe Range          │
│   No action needed       │
└──────────────────────────┘
                           ↓
🟡 ORANGE (26% - 50%)
├──────────────────────────┤
│   Consider refilling     │
│      Plan ahead          │
└──────────────────────────┘
                           ↓
🔴 RED (0% - 25%)
├──────────────────────────┤
│   Refill recommended!    │
│   Running low on fuel    │
└──────────────────────────┘
```

## 📊 Database Relationships

```
┌──────────────────────┐
│      Vehicle         │
│──────────────────────│
│ id (PK)              │←─────────┐
│ name                 │          │
│ model                │          │
│ licensePlate         │          │
│ tankCapacity         │          │
│ currentFuelLevel     │          │
│ createdAt            │          │
│ isActive             │          │
└──────────────────────┘          │
         1                        │
         │                        │
         │ has many              │
         │                        │
         ↓                        │
        Many                      │
┌──────────────────────┐          │
│       Refill         │          │
│──────────────────────│          │
│ id (PK)              │          │
│ vehicleId (FK)       │──────────┘
│ date                 │
│ fuelBefore           │
│ refillAmount         │
│ fuelAfter            │
│ unitPrice            │
│ totalPrice           │
│ odometerReading      │
│ notes                │
│ location             │
└──────────────────────┘

Cascade Delete: YES
When a Vehicle is deleted,
all its Refills are deleted too.
```

---

**These diagrams help visualize the app's architecture, data flow, and user interactions.**

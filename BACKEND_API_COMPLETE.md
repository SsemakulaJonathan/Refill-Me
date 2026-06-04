# 🚀 Backend API Complete with Vehicles & Refills

## ✅ All Features Implemented

### Database Tables Created

✅ **Users Table** - Authentication and user management
✅ **Vehicles Table** - Complete vehicle information with all fields
✅ **Refills Table** - Fuel refill tracking linked to vehicles

### API Endpoints

#### Authentication
- `POST /api/auth/signup` - Create new user account
- `POST /api/auth/login` - Login with email/password
- `GET /api/auth/verify` - Verify JWT token

#### Vehicles (Requires Authentication)
- `GET /api/vehicles` - List all vehicles for authenticated user
- `POST /api/vehicles` - Create new vehicle
- `PUT /api/vehicles?id={id}` - Update vehicle by ID
- `DELETE /api/vehicles?id={id}` - Delete vehicle by ID

#### Refills (Requires Authentication)
- `GET /api/refills` - List all refills for authenticated user
- `GET /api/refills?vehicleId={id}` - List refills for specific vehicle
- `POST /api/refills` - Create new refill
- `PUT /api/refills?id={id}` - Update refill by ID
- `DELETE /api/refills?id={id}` - Delete refill by ID

#### Photo Search
- `GET /api/search-photos?query={search}` - Search Unsplash for vehicle photos

---

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Vehicles Table
```sql
CREATE TABLE vehicles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    make VARCHAR(255),
    model VARCHAR(255),
    year INTEGER,
    license_plate VARCHAR(100),
    vin VARCHAR(100),
    insurance_number VARCHAR(100),
    fuel_type VARCHAR(50),
    fuel_capacity REAL NOT NULL,
    photo_uri TEXT,
    document1_uri TEXT,
    document2_uri TEXT,
    document3_uri TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Refills Table
```sql
CREATE TABLE refills (
    id SERIAL PRIMARY KEY,
    vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date TIMESTAMP NOT NULL,
    odometer_reading REAL NOT NULL,
    fuel_amount REAL NOT NULL,
    price_per_unit REAL NOT NULL,
    total_cost REAL NOT NULL,
    filling_station VARCHAR(255),
    is_full_tank BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔐 Authentication

All vehicle and refill endpoints require authentication via JWT Bearer token:

```
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

Get token from `/api/auth/login` or `/api/auth/signup` response.

---

## 📸 Photo Search Feature

### Backend API
- Integrated with **Unsplash API**
- Returns high-quality vehicle photos
- Searches automatically with "car vehicle" appended to query

### Android App
- ✅ Photo search dialog implemented
- ✅ Grid layout with 2 columns
- ✅ Clickable thumbnails
- ✅ Shows 20 photos per search
- ✅ Attribution to Unsplash photographers
- ✅ Can select photo from gallery OR search online

### Usage in App
1. Open Add Vehicle screen
2. Click "Search Online" button
3. Enter vehicle make/model (e.g., "Toyota Camry")
4. Browse photos in grid
5. Tap photo to select
6. Photo URL saved with vehicle

---

## 🔧 API Examples

### Create Vehicle
```bash
curl -X POST https://refill-me.vercel.app/api/vehicles \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Car",
    "make": "Toyota",
    "model": "Camry",
    "year": 2020,
    "licensePlate": "ABC123",
    "vin": "1HGBH41JXMN109186",
    "insuranceNumber": "INS123456",
    "fuelType": "PETROL",
    "fuelCapacity": 60.0,
    "photoUri": "https://images.unsplash.com/photo-123...",
    "notes": "Company car"
  }'
```

### Get All Vehicles
```bash
curl https://refill-me.vercel.app/api/vehicles \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Create Refill
```bash
curl -X POST https://refill-me.vercel.app/api/refills \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": 1,
    "date": "2025-12-23T10:00:00Z",
    "odometerReading": 50000.0,
    "fuelAmount": 45.0,
    "pricePerUnit": 1500.0,
    "totalCost": 67500.0,
    "fillingStation": "Shell Station",
    "isFullTank": true,
    "notes": "Regular refill"
  }'
```

### Search Photos
```bash
curl "https://refill-me.vercel.app/api/search-photos?query=toyota+camry"
```

---

## 📱 Android App Features

### Implemented
✅ Photo search dialog with Unsplash integration
✅ PhotoApiService for backend communication
✅ Grid layout for photo results
✅ Photo selection and URL storage
✅ Support for both local photos and online URLs

### Components Created
- `PhotoSearchDialog.kt` - Full-screen search dialog
- `PhotoApiService.kt` - API service for photo search
- Updated `AddVehicleScreen.kt` - Integrated photo search button

---

## 🌐 Backend Deployed

**Production URL:** https://refill-me.vercel.app

### Environment Variables
- `PGHOST` - Neon Postgres host
- `PGDATABASE` - Database name
- `PGUSER` - Database user
- `PGPASSWORD` - Database password
- `PGSSLMODE` - SSL mode (require)
- `JWT_SECRET` - JWT signing secret
- `UNSPLASH_ACCESS_KEY` - Unsplash API key

---

## ✅ What's Working

| Feature | Backend | Android App |
|---------|---------|-------------|
| User signup/login | ✅ | ✅ |
| JWT authentication | ✅ | ✅ |
| Vehicle CRUD | ✅ | ⏳ (still using Room) |
| Refill CRUD | ✅ | ⏳ (still using Room) |
| Photo search | ✅ | ✅ |
| Database schemas | ✅ | N/A |
| Authorization checks | ✅ | N/A |

---

## 🔄 Next Steps

### To Complete Full Backend Integration:

1. **Create VehicleApiService.kt**
   - Implement CRUD operations
   - Add authentication headers
   - Handle responses

2. **Create RefillApiService.kt**
   - Implement CRUD operations
   - Add authentication headers
   - Handle responses

3. **Update ViewModels**
   - Replace Room calls with API calls
   - Handle loading states
   - Error handling

4. **Sync Strategy**
   - Decide: Backend-only or Hybrid (Room + Backend)?
   - Implement offline support if needed
   - Handle sync conflicts

---

## 📊 Database Relationships

```
users
  └── vehicles (user_id FK)
        └── refills (vehicle_id FK)
  └── refills (user_id FK)
```

- Users can have multiple vehicles
- Vehicles can have multiple refills
- Refills belong to both a vehicle and a user
- Deleting a user cascades to vehicles and refills
- Deleting a vehicle cascades to refills

---

## 🎯 Testing

### Test Photo Search
```bash
# Search for Toyota Camry
curl "https://refill-me.vercel.app/api/search-photos?query=toyota+camry"

# Search for Honda Civic
curl "https://refill-me.vercel.app/api/search-photos?query=honda+civic"
```

### Test Vehicles (with auth)
```bash
# 1. Login first
TOKEN=$(curl -X POST https://refill-me.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jonathan@test.com","password":"test123456"}' | jq -r '.token')

# 2. Create vehicle
curl -X POST https://refill-me.vercel.app/api/vehicles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Toyota Camry",
    "make":"Toyota",
    "model":"Camry",
    "year":2020,
    "licensePlate":"UBG123A",
    "fuelType":"PETROL",
    "fuelCapacity":60.0
  }'

# 3. List vehicles
curl https://refill-me.vercel.app/api/vehicles \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🎉 Summary

Your Refill Me backend is now feature-complete with:

✅ **3 database tables** (users, vehicles, refills)
✅ **12 API endpoints** (auth, vehicles, refills, photos)
✅ **Photo search** with Unsplash integration
✅ **JWT authentication** with token expiry
✅ **Full CRUD operations** for vehicles and refills
✅ **Authorization checks** - users can only access their own data
✅ **Cascade deletes** - maintain data integrity
✅ **Android app updated** with photo search feature

**App installed:** ✅ Successfully built and installed on Samsung Galaxy S24 Ultra

**Ready to use:** Open app → Add vehicle → Click "Search Online" → Browse photos! 🚗📸

# Debugging Guide for Refill Me

This guide helps you troubleshoot common issues with vehicle creation and login.

## Quick Diagnosis

### Test the Backend API

Run the test script to verify your backend is working:

```bash
cd "/Users/jonathan/SIMI/Refill Me"
./test-backend.sh
```

This will:
1. Test the API health check
2. Create a test user
3. Create a test vehicle
4. Verify vehicles can be retrieved

## Common Issues

### Issue 1: Vehicles Not Saving

**Symptoms:**
- Click "Save Vehicle" but nothing happens
- Screen doesn't navigate back
- No error message appears

**Possible Causes:**

#### A. Backend Not Accessible
```bash
# Test if backend is reachable
curl https://refill-me.vercel.app/api
```

Expected response:
```json
{
  "name": "Refill Me API",
  "version": "1.0.0",
  "endpoints": {...},
  "status": "healthy"
}
```

**Fix:** If this fails, your backend might be down. Check Vercel dashboard.

#### B. Database Tables Not Created
```bash
# Initialize database tables
curl https://refill-me.vercel.app/api/init-tables
```

Expected response:
```json
{
  "success": true,
  "message": "All database tables initialized successfully",
  "tables": ["users", "vehicles", "refills", "vehicle_specifications"]
}
```

**Fix:** Run this command to create all required tables.

#### C. Authentication Token Issues

**Check in Android Studio Logcat:**
- Filter by "VehicleViewModel"
- Look for errors like "Not authenticated" or "Failed to create vehicle"

**Fix:**
1. Log out and log back in
2. Check if token is being saved properly
3. Verify AuthManager is working

#### D. Network Permission Issues

**Check AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Issue 2: Can't See Vehicles After Login

**Symptoms:**
- Login successful
- Dashboard shows "No vehicles" even though you created some
- Vehicle count shows 0

**Possible Causes:**

#### A. Vehicles Created by Different User

Vehicles are user-specific. If you:
1. Created vehicles with user A
2. Logged in as user B

You won't see user A's vehicles.

**Fix:** Log in with the same account you used to create vehicles.

#### B. API Not Returning Vehicles

**Test manually:**
```bash
# Login first
TOKEN=$(curl -s -X POST https://refill-me.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' | jq -r '.token')

# Get vehicles
curl -X GET https://refill-me.vercel.app/api/vehicles \
  -H "Authorization: Bearer $TOKEN"
```

**Fix:** If this returns empty array but you have vehicles, check database directly in Neon dashboard.

#### C. VehicleViewModel Not Loading

**Check Logcat for:**
```
E/VehicleViewModel: Failed to load vehicles
```

**Fix:**
1. Ensure `loadVehicles()` is called on app start
2. Check if authManager has valid token
3. Verify network connectivity

## Debugging Steps

### Step 1: Enable Verbose Logging

In [VehicleViewModel.kt](app/src/main/java/com/simi/refillme/ui/viewmodel/VehicleViewModel.kt), add more logging:

```kotlin
fun loadVehicles() {
    viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            val token = authManager.getToken()
            Log.d("VehicleViewModel", "Token: ${token?.take(10)}...")  // Add this

            if (token != null) {
                val result = vehicleApiService.getVehicles(token)
                Log.d("VehicleViewModel", "API Result: $result")  // Add this

                result.onSuccess { response ->
                    Log.d("VehicleViewModel", "Response: $response")  // Add this
                    // ... rest of code
                }
            }
        } catch (e: Exception) {
            Log.e("VehicleViewModel", "Error loading vehicles", e)
        }
    }
}
```

### Step 2: Check Network Calls

In Android Studio:
1. View → Tool Windows → Logcat
2. Filter by "Ktor" or "HTTP"
3. Look for requests to `refill-me.vercel.app`

You should see:
```
POST https://refill-me.vercel.app/api/auth/login
GET https://refill-me.vercel.app/api/vehicles
POST https://refill-me.vercel.app/api/vehicles
```

### Step 3: Verify Database Schema

Visit Neon dashboard and run:

```sql
-- Check if vehicles table exists
SELECT * FROM information_schema.tables
WHERE table_name = 'vehicles';

-- Check vehicle count
SELECT COUNT(*) FROM vehicles;

-- Check vehicles for specific user
SELECT * FROM vehicles WHERE user_id = 1;
```

### Step 4: Test API Directly

Use the test script or Postman/curl to test each endpoint independently.

## Error Messages Reference

### "Not authenticated"
- **Cause:** No auth token or invalid token
- **Fix:** Log out and log back in

### "Failed to create vehicle"
- **Cause:** Backend validation error or database issue
- **Fix:** Check backend logs in Vercel dashboard

### "Vehicle name and fuel capacity are required"
- **Cause:** Missing required fields in API request
- **Fix:** Ensure form validation is working

### Network timeout or connection errors
- **Cause:** Backend unreachable or network issues
- **Fix:** Check internet connection and backend status

## Verification Checklist

After making changes, verify:

- [ ] Backend health check returns success
- [ ] Database tables exist (run init-tables)
- [ ] Can signup new user
- [ ] Can login with credentials
- [ ] Token is saved and retrieved
- [ ] Can create vehicle (returns success)
- [ ] Can retrieve vehicles list
- [ ] Vehicles appear in app dashboard
- [ ] Logout and login shows same vehicles

## Getting More Help

If issues persist:

1. **Collect logs:**
   - Android Studio Logcat (filter: VehicleViewModel, Ktor)
   - Backend logs from Vercel dashboard
   - Network request/response bodies

2. **Check these files:**
   - [VehicleViewModel.kt](app/src/main/java/com/simi/refillme/ui/viewmodel/VehicleViewModel.kt)
   - [VehicleApiService.kt](app/src/main/java/com/simi/refillme/data/api/VehicleApiService.kt)
   - [vehicles.js](backend/api/vehicles.js)

3. **Verify configuration:**
   - Backend URL in VehicleApiService: `https://refill-me.vercel.app/api`
   - Database connection in Neon dashboard
   - Environment variables in Vercel

## Quick Fixes Summary

| Problem | Quick Fix |
|---------|-----------|
| Vehicles not saving | Run `curl https://refill-me.vercel.app/api/init-tables` |
| Can't see vehicles | Log in with correct account |
| Network errors | Check backend URL and internet connection |
| Authentication errors | Log out and log back in |
| Database errors | Check Neon dashboard for table structure |

## Test Accounts

For testing, you can create accounts with:
- Email: `test+1@example.com`, `test+2@example.com`, etc.
- Password: Any password (minimum 6 characters recommended)

Each account maintains separate vehicles.

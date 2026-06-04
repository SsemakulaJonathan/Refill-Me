# Serialization Fix - LinkedHashMap Error

## Problem

When trying to create a vehicle in the Android app, you got this error:

```
Failed to save vehicle

Serializer for subclass 'LinkedHashMap' is not found in the polymorphic scope of 'Map'.
Check if class with serial name 'LinkedHashMap' exists and serializer is registered in a corresponding SerializersModule.
```

## Root Cause

The issue was in how we were sending data to the API. The code was using `mapOf()` to create request bodies:

```kotlin
// ❌ WRONG - Creates LinkedHashMap which can't be serialized
setBody(mapOf(
    "name" to vehicle.name,
    "make" to vehicle.make,
    // ...
))
```

Kotlinx.serialization cannot serialize a raw `LinkedHashMap` (which is what `mapOf()` creates). It needs properly annotated `@Serializable` data classes.

## Solution

Created `@Serializable` data classes for all API requests and used them instead of `mapOf()`.

### Files Fixed

#### 1. VehicleApiService.kt

**Added:**
```kotlin
@Serializable
data class CreateVehicleRequest(
    val name: String,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val vin: String? = null,
    val insuranceNumber: String? = null,
    val fuelType: String,
    val fuelCapacity: Double,
    val photoUri: String? = null,
    val document1Uri: String? = null,
    val document2Uri: String? = null,
    val document3Uri: String? = null,
    val notes: String? = null
)
```

**Changed:**
```kotlin
// ✅ CORRECT - Uses @Serializable data class
suspend fun createVehicle(token: String, vehicle: Vehicle): Result<VehicleResponse> {
    val request = CreateVehicleRequest(
        name = vehicle.name,
        make = vehicle.make,
        // ...
    )

    val response = client.post("$baseUrl/vehicles") {
        header("Authorization", "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody(request) // Now using data class instead of map
    }
    // ...
}
```

#### 2. SpecificationApiService.kt

**Added:**
```kotlin
@Serializable
data class CreateSpecificationRequest(
    val vehicleId: Long,
    val name: String,
    val value: String
)

@Serializable
data class UpdateSpecificationRequest(
    val name: String,
    val value: String
)
```

**Updated:** `createSpecification()` and `updateSpecification()` functions

#### 3. RefillApiService.kt

**Added:**
```kotlin
@Serializable
data class CreateRefillRequest(
    val vehicleId: Long,
    val date: String,
    val odometerReading: Double,
    val fuelAmount: Double,
    val pricePerUnit: Double,
    val totalCost: Double,
    val fillingStation: String? = null,
    val isFullTank: Boolean = false,
    val notes: String? = null
)
```

**Updated:** `createRefill()` and `updateRefill()` functions

## How to Test

1. **Clean and rebuild the Android app:**
   ```bash
   cd "/Users/jonathan/SIMI/Refill Me"
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

3. **Test vehicle creation:**
   - Open the app
   - Login or signup
   - Click the FAB to add a vehicle
   - Fill in the details
   - Click "Save Vehicle"
   - **Expected:** Vehicle saves successfully and you're taken back to the dashboard
   - **Expected:** Vehicle appears in the dashboard

## Why This Works

Kotlinx.serialization requires classes to be explicitly marked as `@Serializable`. When you use:

- ❌ `mapOf()` → Creates `LinkedHashMap` → Not serializable → Error
- ✅ `@Serializable data class` → Kotlinx.serialization can handle it → Success

The serialization library generates serializers at compile time for classes marked with `@Serializable`, but it can't do this for generic collections like `Map` or `LinkedHashMap` without additional configuration.

## Additional Benefits

Using data classes instead of maps provides:

1. **Type safety** - Compiler checks field types
2. **Better IDE support** - Autocomplete and refactoring
3. **Clear API contract** - Easy to see what fields are required
4. **Null safety** - Explicit nullable fields with `?`
5. **Default values** - Optional parameters with defaults

## Files Modified

- ✅ [app/src/main/java/com/simi/refillme/data/api/VehicleApiService.kt](app/src/main/java/com/simi/refillme/data/api/VehicleApiService.kt)
- ✅ [app/src/main/java/com/simi/refillme/data/api/SpecificationApiService.kt](app/src/main/java/com/simi/refillme/data/api/SpecificationApiService.kt)
- ✅ [app/src/main/java/com/simi/refillme/data/api/RefillApiService.kt](app/src/main/java/com/simi/refillme/data/api/RefillApiService.kt)

## Summary

**Before:** Using `mapOf()` → LinkedHashMap → Serialization error ❌

**After:** Using `@Serializable` data classes → Clean serialization → Success ✅

Now when you try to create a vehicle, it should work perfectly!

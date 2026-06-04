# Troubleshooting Guide - Refill Me

## 🔧 Common Issues & Solutions

### Build Issues

#### ❌ Problem: Gradle Sync Failed
**Symptoms:**
- Red errors in build.gradle files
- "Sync failed" message in Android Studio
- Can't build or run the app

**Solutions:**
1. **Check Internet Connection**
   - Gradle needs to download dependencies
   
2. **Invalidate Caches**
   - File → Invalidate Caches → Invalidate and Restart
   
3. **Clean & Rebuild**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```
   
4. **Check Gradle JDK**
   - File → Project Structure → SDK Location
   - Ensure JDK 17 is selected
   
5. **Update Gradle Wrapper** (if needed)
   ```bash
   ./gradlew wrapper --gradle-version=8.2
   ```

#### ❌ Problem: Room Schema Export Error
**Error Message:** "Cannot find schema export directory"

**Solution:**
This is expected! The app uses `exportSchema = false` in the database configuration, so no action needed.

#### ❌ Problem: KSP Plugin Not Found
**Error Message:** "Plugin [id: 'com.google.devtools.ksp'] was not found"

**Solution:**
1. Check `build.gradle.kts` (root) has KSP plugin
2. Ensure version matches Kotlin version:
   ```kotlin
   id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
   ```
3. Sync project again

---

### Runtime Issues

#### ❌ Problem: App Crashes on Launch
**Check Logcat for:**
- `java.lang.RuntimeException: Unable to start activity`
- Database errors
- Missing dependencies

**Solutions:**
1. **Uninstall & Reinstall**
   - Complete clean installation
   ```bash
   adb uninstall com.simi.refillme
   ./gradlew installDebug
   ```

2. **Check AndroidManifest.xml**
   - Verify application name matches: `RefillMeApplication`
   
3. **Clear App Data**
   - Device Settings → Apps → Refill Me → Storage → Clear Data

#### ❌ Problem: Database Crash
**Error:** "Cannot access database on the main thread"

**This shouldn't happen** - we use Coroutines everywhere. If it does:
1. Check all database calls are in `suspend` functions
2. Verify ViewModelScope is used for database operations
3. Look for any direct DAO calls from composables

#### ❌ Problem: Empty Screen / No Data Showing
**Symptoms:**
- Blank lists
- "No vehicles" even after adding

**Solutions:**
1. **Check Database**
   - Use Android Studio Database Inspector
   - View → Tool Windows → App Inspection → Database Inspector
   
2. **Verify Flow Collection**
   - Ensure `.collectAsState()` is used in composables
   
3. **Add Debug Logs**
   ```kotlin
   Log.d("RefillMe", "Vehicles: ${vehicles.size}")
   ```

---

### UI Issues

#### ❌ Problem: Buttons Not Clickable
**Possible Causes:**
- Overlapping UI elements
- Disabled state

**Solutions:**
1. Check if vehicle is selected (for Add Refill)
2. Verify all required fields are filled
3. Look for any modal dialogs covering the screen

#### ❌ Problem: Auto-Calculation Not Working
**Symptoms:**
- Enter 2 values, third doesn't calculate

**Debug Steps:**
1. **Check Values Are Valid Numbers**
   - No letters or special characters
   - Use decimal point (.) not comma (,)
   
2. **Verify LaunchedEffect**
   ```kotlin
   LaunchedEffect(formState.refillAmount, formState.unitPrice, formState.totalPrice) {
       // This should trigger when any field changes
   }
   ```
   
3. **Check for Division by Zero**
   - Unit price or amount can't be zero

#### ❌ Problem: Fuel Gauge Not Updating
**After adding a refill, vehicle fuel level doesn't change**

**Solutions:**
1. **Check Save Logic**
   - Verify `vehicleRepository.updateFuelLevel()` is called
   
2. **Inspect Database**
   - Database Inspector → vehicles table
   - Check `currentFuelLevel` column updates
   
3. **Verify Flow Updates**
   - Flow should emit new values automatically

---

### Device/Emulator Issues

#### ❌ Problem: Can't Connect Device
**USB Debugging not working**

**Solutions:**
1. **Enable Developer Options**
   - Settings → About Phone → Tap "Build Number" 7 times
   
2. **Enable USB Debugging**
   - Settings → Developer Options → USB Debugging → ON
   
3. **Trust Computer**
   - Accept prompt on device when connecting
   
4. **Check USB Cable**
   - Use data cable, not charge-only cable
   
5. **Try Different USB Port**
   - Some ports may not work well

#### ❌ Problem: Emulator Won't Start
**AVD (Android Virtual Device) issues**

**Solutions:**
1. **Check Available Disk Space**
   - Need at least 10GB free
   
2. **Update System Images**
   - SDK Manager → System Images → Update
   
3. **Recreate AVD**
   - AVD Manager → Delete → Create New
   - Recommended: Pixel 4, API 30+
   
4. **Enable Hardware Acceleration**
   - macOS: Ensure Apple Silicon or Intel HAXM is enabled
   
5. **Cold Boot**
   - AVD Manager → Actions → Cold Boot Now

---

### Data Issues

#### ❌ Problem: Tank Overfill Error
**Error:** "Total fuel exceeds tank capacity"

**This is working correctly!** The app prevents you from adding more fuel than the tank can hold.

**Solutions:**
1. **Check Current Fuel Level**
   - Maybe you entered wrong "fuel before" value
   
2. **Verify Tank Capacity**
   - Go to Vehicles → Check tank capacity is correct
   
3. **Recalculate**
   - fuelBefore + refillAmount must be ≤ tankCapacity

#### ❌ Problem: Wrong Fuel Level Displayed
**Current fuel level doesn't match reality**

**Solutions:**
1. **Manual Correction**
   - Currently, you can't edit directly
   - Workaround: Add a refill with amount = 0 to update level
   
2. **Future Feature**
   - Edit vehicle fuel level feature planned

#### ❌ Problem: Lost All Data
**App reset, all vehicles and refills gone**

**Possible Causes:**
1. **App Uninstalled/Data Cleared**
   - SQLite data is in app storage
   
2. **Database Migration Failed**
   - Update might have triggered destructive migration
   
**Prevention:**
- Future versions will include backup/export feature

---

### Performance Issues

#### ⚠️ Problem: App is Slow
**UI lags, slow responses**

**Unlikely with small datasets, but if it happens:**

1. **Check Database Size**
   - Database Inspector → Check row counts
   
2. **Profile Performance**
   - Android Studio → Profiler → CPU
   
3. **Limit Displayed Items**
   - Dashboard shows only recent 5 refills (working as designed)

#### ⚠️ Problem: High Battery Usage
**App draining battery**

**Solutions:**
1. **Check Background Processes**
   - App shouldn't run in background
   - Kill and restart if needed
   
2. **No Network Calls**
   - App is fully offline, no network activity

---

### Development Issues

#### ❌ Problem: Can't Edit Code / Locked Files
**IDE shows files as read-only**

**Solutions:**
1. **Check File Permissions**
   ```bash
   chmod -R u+w "/Users/jonathan/SIMI/Refill Me"
   ```
   
2. **Close and Reopen Project**
   - File → Close Project
   - Reopen from welcome screen

#### ❌ Problem: Code Not Compiling After Changes
**Syntax errors or unresolved references**

**Solutions:**
1. **Invalidate Caches**
   - File → Invalidate Caches → Invalidate and Restart
   
2. **Reimport Project**
   - File → Sync Project with Gradle Files
   
3. **Check Imports**
   - Make sure all required imports are present
   
4. **Rebuild Project**
   - Build → Rebuild Project

---

## 🔍 Debugging Tips

### Enable Verbose Logging
Add to any ViewModel or Repository:
```kotlin
import android.util.Log

private val TAG = "RefillMe"

Log.d(TAG, "Method called with params: $params")
Log.e(TAG, "Error occurred", exception)
```

### Use Database Inspector
1. Run app in debug mode
2. View → Tool Windows → App Inspection
3. Select Database Inspector
4. View/edit database in real-time

### Check Logcat Filters
In Logcat panel:
- Filter by: `package:com.simi.refillme`
- Or tag: `RefillMe`

### Breakpoint Debugging
1. Click left margin to add breakpoint
2. Run → Debug 'app'
3. Step through code when breakpoint hits

---

## 📞 Getting Help

### Before Asking for Help:
1. ✅ Check this troubleshooting guide
2. ✅ Look at Logcat for error messages
3. ✅ Try clean & rebuild
4. ✅ Restart Android Studio

### When Reporting Issues:
Include:
- Android Studio version
- Device/Emulator details
- Full error message from Logcat
- Steps to reproduce
- Screenshots if UI-related

---

## 🎯 Quick Fixes Checklist

When something isn't working, try these in order:

1. ☑️ Clean & Rebuild
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

2. ☑️ Invalidate Caches
   ```
   File → Invalidate Caches → Invalidate and Restart
   ```

3. ☑️ Sync Gradle
   ```
   File → Sync Project with Gradle Files
   ```

4. ☑️ Restart App
   ```
   Stop app and run again
   ```

5. ☑️ Reinstall App
   ```
   Uninstall from device/emulator
   Run again
   ```

6. ☑️ Restart Android Studio
   ```
   Complete restart
   ```

7. ☑️ Restart Computer
   ```
   Sometimes helps with system-level issues
   ```

---

**Most issues can be resolved with a clean rebuild and cache invalidation! 🎉**

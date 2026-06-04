# 🏗️ Build Instructions - Refill Me

Complete guide to building and running the Refill Me Android app.

---

## 📋 Prerequisites

### Required Software

#### 1. Android Studio
- **Version**: Hedgehog (2023.1.1) or newer
- **Download**: https://developer.android.com/studio
- **Installation**: Follow platform-specific instructions

#### 2. Java Development Kit (JDK)
- **Version**: JDK 17 (Required)
- **Check current version**:
  ```bash
  java -version
  ```
- **macOS Installation** (if needed):
  ```bash
  brew install openjdk@17
  ```

#### 3. Android SDK
- **Components needed**:
  - Android SDK Platform 34
  - Android SDK Build-Tools 34.0.0
  - Android SDK Platform-Tools
  - Android Emulator (optional, for testing)

### System Requirements

#### macOS
- macOS 10.14 (Mojave) or higher
- 8 GB RAM minimum (16 GB recommended)
- 10 GB free disk space
- Intel or Apple Silicon processor

#### Windows
- Windows 10 64-bit or higher
- 8 GB RAM minimum (16 GB recommended)
- 10 GB free disk space

#### Linux
- Ubuntu 18.04 or higher (or equivalent)
- 8 GB RAM minimum
- 10 GB free disk space

---

## 🚀 Setup Instructions

### Step 1: Open the Project

#### Option A: Already at Project Location
```bash
cd "/Users/jonathan/SIMI/Refill Me"
```

#### Option B: Using Android Studio
1. Launch Android Studio
2. Click **"Open"** from Welcome screen
3. Navigate to: `/Users/jonathan/SIMI/Refill Me`
4. Click **"Open"**

### Step 2: Wait for Gradle Sync

Android Studio will automatically:
- ✅ Download required dependencies
- ✅ Index project files
- ✅ Configure build tools
- ✅ Setup Kotlin compiler

**This may take 2-10 minutes on first run.**

#### If Sync Fails:
```
File → Invalidate Caches → Invalidate and Restart
```

### Step 3: Configure Android SDK

1. **File → Project Structure** (or `Cmd + ;` on Mac)
2. **SDK Location**
   - Verify SDK path is set
   - Should be: `/Users/YOUR_USERNAME/Library/Android/sdk`
3. **Gradle JDK**
   - Select: JDK 17
4. Click **"OK"**

---

## 📱 Running the App

### On Physical Device

#### Prerequisites:
1. **Enable Developer Options**
   - Settings → About Phone
   - Tap "Build Number" 7 times
   - Message: "You are now a developer!"

2. **Enable USB Debugging**
   - Settings → Developer Options
   - Toggle "USB Debugging" ON

3. **Connect Device**
   - Use USB cable (data transfer capable)
   - Accept "Allow USB Debugging" prompt on device

#### Run:
1. Click green ▶️ **Run** button (or `Ctrl + R`)
2. Select your device from the list
3. Click **"OK"**
4. Wait for build and installation (~30-60 seconds)

### On Android Emulator

#### Create Emulator:
1. **Tools → Device Manager** (or AVD Manager icon)
2. Click **"Create Device"**
3. **Select Hardware**:
   - Category: Phone
   - Device: Pixel 4 or Pixel 5
   - Click **"Next"**
4. **System Image**:
   - Release: R (API 30) or higher
   - Download if needed
   - Click **"Next"**
5. **Verify Configuration**:
   - AVD Name: (default is fine)
   - Click **"Finish"**

#### Run on Emulator:
1. Click green ▶️ **Run** button
2. Select created emulator
3. Click **"OK"**
4. Emulator will start (may take 1-2 minutes)
5. App installs automatically

---

## 🔨 Build Commands

### Using Gradle (Command Line)

#### Navigate to Project:
```bash
cd "/Users/jonathan/SIMI/Refill Me"
```

#### Build Debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Build Release APK:
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

#### Install on Connected Device:
```bash
./gradlew installDebug
```

#### Clean Build:
```bash
./gradlew clean
./gradlew assembleDebug
```

#### Run Tests:
```bash
./gradlew test
```

---

## 🔍 Build Verification

### Check Build Success

#### In Android Studio:
- **Build** tab shows: `BUILD SUCCESSFUL`
- No errors in **Problems** tab
- Green checkmark on Run button

#### Command Line:
```bash
./gradlew assembleDebug

# Should see:
# BUILD SUCCESSFUL in Xs Ys
```

### Verify App Installation

#### On Device/Emulator:
1. App drawer shows "Refill Me" icon
2. Tap to open
3. Dashboard screen appears
4. No crashes

---

## 🐛 Troubleshooting Build Issues

### Common Problems

#### ❌ Gradle Sync Failed

**Error**: "Sync failed: ..."

**Solutions**:
```bash
# 1. Clean and rebuild
./gradlew clean

# 2. In Android Studio
File → Invalidate Caches → Invalidate and Restart

# 3. Delete .gradle folders
rm -rf .gradle
rm -rf app/.gradle
# Then sync again in Android Studio
```

#### ❌ JDK Version Mismatch

**Error**: "Unsupported class file major version"

**Solution**:
1. File → Project Structure
2. SDK Location → Gradle JDK
3. Select: JDK 17
4. Click OK → Sync Project

#### ❌ SDK Not Found

**Error**: "SDK location not found"

**Solution**:
1. Download Android SDK via SDK Manager
2. Set SDK path:
   - File → Project Structure → SDK Location
3. Browse to SDK location
4. Click OK

#### ❌ Dependency Download Failed

**Error**: "Could not resolve all dependencies"

**Solutions**:
- Check internet connection
- Try VPN if blocked
- Clear Gradle cache:
  ```bash
  rm -rf ~/.gradle/caches
  ```

#### ❌ Build Takes Too Long

**Issue**: Build hangs or very slow

**Solutions**:
1. Increase Gradle memory:
   Edit `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
   ```

2. Enable parallel builds (already set):
   ```properties
   org.gradle.parallel=true
   ```

3. Close other applications to free RAM

---

## 📦 APK Installation

### Debug APK (For Testing)

#### Build:
```bash
./gradlew assembleDebug
```

#### Install:
```bash
# Option 1: Via Gradle
./gradlew installDebug

# Option 2: Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Option 3: Manual
# 1. Copy APK to device
# 2. Open file manager on device
# 3. Tap APK to install
# 4. Allow "Install from Unknown Sources" if prompted
```

### Release APK (For Distribution)

#### Sign the APK:
1. **Build → Generate Signed Bundle/APK**
2. Select **APK**
3. Create or select keystore
4. Enter passwords
5. Select **release** build variant
6. Click **Finish**

Output: `app/release/app-release.apk`

---

## 🧪 Testing the Build

### Manual Testing Checklist

#### Initial Launch:
- [ ] App opens without crash
- [ ] Dashboard displays
- [ ] No error messages

#### Add Vehicle:
- [ ] Tap FAB button
- [ ] Dialog appears
- [ ] Fill form and save
- [ ] Vehicle appears on dashboard

#### Add Refill:
- [ ] Tap FAB button
- [ ] Select vehicle
- [ ] Enter 2 values
- [ ] Third value auto-calculates
- [ ] Save successful
- [ ] Refill appears on dashboard

#### Navigation:
- [ ] All screen transitions smooth
- [ ] Back button works
- [ ] No crashes during navigation

---

## 🔧 Development Build Configuration

### Debug Build (Default)

**Characteristics**:
- ✅ Debuggable
- ✅ Includes debug symbols
- ✅ No code optimization
- ✅ Instant Run enabled
- ⚠️ Larger APK size (~10-15 MB)

**Use for**:
- Development
- Testing
- Debugging

### Release Build

**Characteristics**:
- ✅ Code optimized (ProGuard)
- ✅ Smaller APK size (~5-8 MB)
- ✅ Better performance
- ❌ Not debuggable
- ❌ Requires signing

**Use for**:
- Production
- App store submission
- User distribution

---

## 📊 Build Performance Tips

### Speed Up Builds

1. **Increase Gradle Memory**
   ```properties
   # gradle.properties
   org.gradle.jvmargs=-Xmx4096m
   ```

2. **Enable Build Cache**
   ```properties
   # gradle.properties (already set)
   org.gradle.caching=true
   ```

3. **Use Gradle Daemon**
   ```properties
   # gradle.properties (already set)
   org.gradle.daemon=true
   ```

4. **Parallel Execution**
   ```properties
   # gradle.properties (already set)
   org.gradle.parallel=true
   ```

5. **Configure On Demand**
   ```bash
   ./gradlew assembleDebug --configure-on-demand
   ```

### Monitor Build Times

```bash
# Build with profiling
./gradlew assembleDebug --profile

# Check report in:
# build/reports/profile/
```

---

## 🎯 Quick Reference

### Essential Commands

```bash
# Clean project
./gradlew clean

# Build debug
./gradlew assembleDebug

# Install debug
./gradlew installDebug

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies

# List tasks
./gradlew tasks
```

### Build Outputs

```
app/build/outputs/
├── apk/
│   ├── debug/
│   │   └── app-debug.apk
│   └── release/
│       └── app-release.apk
└── logs/
    └── manifest-merger-*.txt
```

---

## ✅ Build Checklist

### Before Building:
- [ ] Android Studio installed
- [ ] JDK 17 installed
- [ ] Android SDK 34 installed
- [ ] Project opened successfully
- [ ] Gradle sync completed

### For First Build:
- [ ] Internet connection active
- [ ] Sufficient disk space (10+ GB)
- [ ] No antivirus blocking
- [ ] Gradle downloaded dependencies

### For Device Testing:
- [ ] USB debugging enabled
- [ ] Device connected
- [ ] Device authorized
- [ ] Device appears in device list

### For Release Build:
- [ ] Keystore created
- [ ] Passwords documented safely
- [ ] Version code incremented
- [ ] Version name updated
- [ ] Release notes prepared

---

## 📞 Getting Help

### Build Issues?
1. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. Review error messages in Build tab
3. Check Logcat for runtime errors
4. Search error message online

### Still Stuck?
- Include full error message
- Mention Android Studio version
- Specify operating system
- Share Gradle version

---

## 🎉 Success!

If you see:
```
BUILD SUCCESSFUL in 45s
```

And the app runs without crashes - **Congratulations! 🎊**

You've successfully built Refill Me!

---

**Ready to start adding features? Check out [ROADMAP.md](ROADMAP.md) for ideas! 🚀**

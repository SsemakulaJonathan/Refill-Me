# Refill Me - Quick Start Guide

## 🎯 What Makes This App Special?

Unlike "Simply Auto" which assumes your tank is at 0 litres when you refill, **Refill Me** tracks your actual fuel level and provides intelligent calculations!

## 🚀 Quick Setup (5 Minutes)

### Step 1: Open in Android Studio
1. Launch Android Studio
2. Click **File → Open**
3. Navigate to: `/Users/jonathan/SIMI/Refill Me`
4. Click **OK** and wait for Gradle sync

### Step 2: Run the App
1. Connect your Android device via USB (enable USB debugging)
   - OR start an Android Emulator
2. Click the green ▶️ **Run** button
3. Select your device
4. Wait for the app to install and launch

## 📱 First Time Usage

### Add Your First Vehicle
1. App opens on the Dashboard
2. Tap the blue **floating + button** (bottom right)
3. A dialog appears → Tap **"Add Vehicle"**
4. Fill in details:
   ```
   Vehicle Name: My Car
   Model: Toyota Camry 2020
   License Plate: ABC-1234
   Tank Capacity: 60
   ```
5. Tap **"Save"**

### Record Your First Refill
1. Tap the green **floating + button** again
2. Select your vehicle from the list
3. Notice: **"Fuel Before Refill"** is auto-filled with your current level!

#### Smart Calculation Examples:

**Scenario A**: You got a receipt showing:
- Total paid: `$70.00`
- Litres pumped: `50.5L`

Just enter these two values, and the app calculates:
- ✨ **Unit Price: $1.39/L** (automatically!)

**Scenario B**: Gas station shows price per litre:
- Price: `$1.45/L`
- Total paid: `$85.00`

Enter these, and the app calculates:
- ✨ **Litres: 58.62L** (automatically!)

**Scenario C**: You know the pump details:
- Price: `$1.50/L`
- Litres: `45.0L`

Enter these, and the app calculates:
- ✨ **Total: $67.50** (automatically!)

4. Tap **"Save Refill"**

## 🎨 Understanding the UI

### Dashboard Screen
- **Top Cards**: Quick stats (number of vehicles, total refills)
- **My Vehicles Section**: Shows your vehicles with fuel gauges
  - 🟢 Green bar = More than 50% full
  - 🟡 Orange bar = 25-50% full  
  - 🔴 Red bar = Less than 25% full (time to refill!)
- **Recent Refills**: History of your last 5 refills

### Color Indicators
The app uses colors to help you quickly understand fuel levels:
- **Green**: You're good to go! (>50%)
- **Orange**: Consider refilling soon (25-50%)
- **Red**: Low fuel warning! (<25%)

## 💡 Pro Tips

### Tip 1: Before You Refill
Check the current fuel level in your vehicle card. The app remembers exactly how much fuel you have!

### Tip 2: Partial Information?
You only need **any 2 of the 3 values**:
- Litres + Price per litre
- Litres + Total price
- Price per litre + Total price

The third one calculates automatically! No more math at the gas station! 🎉

### Tip 3: Add Extra Details
While optional, adding these helps track your vehicle better:
- **Odometer Reading**: Track mileage between refills
- **Location**: Remember which gas stations you use
- **Notes**: "Premium fuel" or "Had to use different octane"

### Tip 4: Tank Capacity Check
The app won't let you exceed your tank capacity! If you try to add more fuel than fits, you'll get a helpful error message.

## 🔧 Troubleshooting

### App Won't Build?
1. Check Android Studio shows "Gradle sync successful"
2. Ensure you have Android SDK 34 installed
3. Try: **Build → Clean Project** then **Build → Rebuild Project**

### Can't Run on Device?
1. Enable **Developer Options** on your Android device
2. Enable **USB Debugging**
3. Accept the connection prompt on your device

### Emulator Not Starting?
1. Open **AVD Manager** (Device Manager icon)
2. Create a new Virtual Device
3. Choose Pixel 4 or newer
4. Select system image: Android 11+ (API 30+)

## 📊 What Data is Tracked?

For each refill, the app stores:
- ✅ Which vehicle
- ✅ Date and time
- ✅ Fuel level before refill
- ✅ Amount refilled
- ✅ Fuel level after refill
- ✅ Price per litre
- ✅ Total cost
- ✅ Optional: Odometer, location, notes

All stored **locally** in SQLite - no internet required!

## 🎯 Key Features at a Glance

| Feature | Refill Me | Simply Auto |
|---------|-----------|-------------|
| Track current fuel level | ✅ Yes | ❌ No (assumes 0) |
| Auto-calculate missing values | ✅ Yes | ❌ No |
| Multiple vehicles | ✅ Yes | ✅ Yes |
| Visual fuel indicators | ✅ Yes | ❌ No |
| Modern UI | ✅ Yes | ~ Basic |
| Offline support | ✅ Yes | ✅ Yes |

## 📞 Need Help?

If something isn't working:
1. Check the **logcat** in Android Studio for error messages
2. Verify all required fields are filled
3. Ensure numeric values are valid (no negative numbers)
4. Make sure tank capacity is set correctly

## 🎉 You're Ready!

Start tracking your fuel expenses with accurate data and smart calculations!

---

**Happy Tracking! ⛽️🚗**

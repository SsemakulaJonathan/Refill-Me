# Quick Deploy Fix Guide

## Critical Bug Fixed

**Issue:** All vehicle/refill API endpoints were failing with `FUNCTION_INVOCATION_FAILED` error.

**Cause:** Incorrect import paths in backend files:
- `vehicles.js` was using `require('../db')` instead of `require('./db')`
- `refills.js` was using `require('../db')` instead of `require('./db')`
- `specifications.js` had the same issue (already fixed)

**Files Fixed:**
- ✅ [backend/api/vehicles.js](backend/api/vehicles.js)
- ✅ [backend/api/refills.js](backend/api/refills.js)
- ✅ [backend/api/specifications.js](backend/api/specifications.js)

## Deploy to Vercel

### Option 1: Vercel CLI (Recommended)

```bash
cd "/Users/jonathan/SIMI/Refill Me/backend"
vercel --prod
```

### Option 2: Git Push (If connected to GitHub)

```bash
cd "/Users/jonathan/SIMI/Refill Me"
git add backend/api/vehicles.js backend/api/refills.js backend/api/specifications.js
git commit -m "Fix: Correct import paths for db and middleware in API files"
git push
```

Vercel will automatically deploy when you push to the main branch.

### Option 3: Vercel Dashboard

1. Go to https://vercel.com/dashboard
2. Find your "refill-me" project
3. Click on "Deployments"
4. Click "Redeploy" → "Use existing Build Cache" → "Redeploy"

## Verify the Fix

After deployment, run the test script again:

```bash
cd "/Users/jonathan/SIMI/Refill Me"
./test-backend.sh
```

**Expected output:**
```
✅ Signup successful!
✅ Vehicle created successfully!
```

You should see proper JSON responses instead of errors.

## Quick Test (Without Script)

```bash
# 1. Signup
TOKEN=$(curl -s -X POST https://refill-me.vercel.app/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"quicktest@example.com","password":"test123","fullName":"Quick Test"}' | jq -r '.token')

# 2. Create vehicle
curl -X POST https://refill-me.vercel.app/api/vehicles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Car",
    "make": "Toyota",
    "model": "Camry",
    "year": 2020,
    "licensePlate": "TEST123",
    "fuelType": "PETROL",
    "fuelCapacity": 60
  }' | jq .
```

**Expected:** Should return `{"success": true, "message": "Vehicle created successfully", ...}`

## After Deployment

1. **Test the app:**
   - Open the Android app
   - Log in
   - Try creating a vehicle
   - Vehicle should save successfully
   - You should see the vehicle in the dashboard

2. **Initialize database tables** (if not done yet):
   ```bash
   curl https://refill-me.vercel.app/api/init-tables
   ```

## Troubleshooting

If deployment fails:

1. **Check build logs:**
   - Vercel Dashboard → Your Project → Deployments → Click on latest → View Build Logs

2. **Verify files are correct:**
   ```bash
   grep "require('./db')" backend/api/vehicles.js
   grep "require('./db')" backend/api/refills.js
   grep "require('./db')" backend/api/specifications.js
   ```
   All three should return the line with `require('./db')`

3. **Clear Vercel cache:**
   - In Vercel dashboard, use "Redeploy" without using existing build cache

## What This Fixes

After deploying these changes:

✅ Vehicle creation will work
✅ Vehicle listing will work
✅ Refill creation will work
✅ Specifications feature will work
✅ App will be able to save and retrieve data

The Android app already has all the necessary error handling, so once the backend is deployed, everything should work smoothly!

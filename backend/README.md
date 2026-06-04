# Refill Me Backend API

Backend API for Refill Me mobile app with authentication using Neon Postgres.

## Setup

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Configure Environment Variables

The `.env` file is already configured with your Neon Postgres credentials. For production on Vercel, you'll need to set these as environment variables.

**Important:** Generate a strong JWT secret for production:
```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

### 3. Local Development

```bash
npm run dev
```

Server will run on `http://localhost:3000`

## Deploy to Vercel

### Option 1: Vercel CLI (Recommended)

```bash
# Install Vercel CLI globally
npm install -g vercel

# Login to Vercel
vercel login

# Deploy (from backend directory)
cd backend
vercel

# For production deployment
vercel --prod
```

### Option 2: Vercel Dashboard

1. Go to [vercel.com](https://vercel.com)
2. Click "New Project"
3. Import your Git repository
4. Set root directory to `backend`
5. Add environment variables:
   - `PGHOST`: your Neon host (from Neon dashboard)
   - `PGDATABASE`: your database name
   - `PGUSER`: your database user
   - `PGPASSWORD`: your database password
   - `PGSSLMODE`: `require`
   - `JWT_SECRET`: (generate a strong random string)
6. Deploy!

## API Endpoints

### Health Check
```
GET /api
```

### Signup
```
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "John Doe"
}
```

### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Verify Token
```
GET /api/auth/verify
Authorization: Bearer <token>
```

## Update Android App

After deployment, update `AuthApiService.kt` in your Android app:

```kotlin
private val baseUrl = "https://your-vercel-app.vercel.app/api"
```

Replace `your-vercel-app` with your actual Vercel deployment URL.

## Database Schema

The database schema is automatically created on first run:

- **users** table with fields: id, email, password_hash, full_name, created_at
- Index on email for fast lookups

## Security Features

✅ Passwords hashed with bcrypt (10 rounds)
✅ JWT tokens for authentication (30 day expiry)
✅ SSL/TLS connection to Neon Postgres
✅ CORS enabled for mobile app access
✅ Input validation on all endpoints
✅ SQL injection protection with parameterized queries

## Testing

Test the API with curl:

```bash
# Signup
curl -X POST http://localhost:3000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","fullName":"Test User"}'

# Login
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Verify (replace TOKEN with actual token from login/signup)
curl -X GET http://localhost:3000/api/auth/verify \
  -H "Authorization: Bearer TOKEN"
```

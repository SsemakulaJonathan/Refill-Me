# Neon Postgres Database Setup

## Database Credentials

Copy the values from your Neon dashboard and add them to `backend/.env`:

```bash
PGHOST='your-host.neon.tech'
PGDATABASE='your-database'
PGUSER='your-user'
PGPASSWORD='your-password'
PGSSLMODE='require'
PGCHANNELBINDING='require'
```

## Connection String
```
postgresql://<user>:<password>@<host>/<database>?sslmode=require
```

## Backend API Setup

To complete the authentication system, you need to create a backend API server with the following endpoints:

### Required Endpoints

1. **POST /auth/signup**
   - Request body: `{ email, password, fullName }`
   - Response: `{ success: boolean, message: string, token?: string, user?: { id, email, fullName, createdAt } }`
   - Action: Create new user in database, hash password, generate JWT token

2. **POST /auth/login**
   - Request body: `{ email, password }`
   - Response: `{ success: boolean, message: string, token?: string, user?: { id, email, fullName, createdAt } }`
   - Action: Verify credentials, generate JWT token

3. **GET /auth/verify**
   - Header: `Authorization: Bearer {token}`
   - Response: HTTP 200 if valid, 401 if invalid
   - Action: Verify JWT token

### Database Schema

Create a `users` table in your Neon database:

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

### Backend Implementation Options

#### Option 1: Node.js + Express

```bash
npm init -y
npm install express pg bcrypt jsonwebtoken dotenv cors
```

#### Option 2: Python + FastAPI

```bash
pip install fastapi uvicorn psycopg2-binary bcrypt pyjwt python-dotenv
```

#### Option 3: Deploy on Vercel/Railway/Render
- Use serverless functions
- Connect to Neon Postgres
- Deploy API endpoints

## Next Steps

1. **Create Backend Project**
   - Choose your preferred backend framework
   - Set up Express/FastAPI/etc.
   - Install required dependencies

2. **Implement Authentication Endpoints**
   - Hash passwords with bcrypt
   - Generate JWT tokens
   - Validate credentials

3. **Deploy Backend**
   - Deploy to Vercel, Railway, Render, or Heroku
   - Get your API URL (e.g., `https://your-api.vercel.app`)

4. **Update Android App**
   - Open `AuthApiService.kt`
   - Replace `https://your-neon-api-endpoint.com/api` with your actual API URL
   - Rebuild and test the app

## Testing Authentication

Once backend is deployed, test the flow:

1. Open app → Should show Login screen
2. Tap "Sign up" → Fill form → Create account
3. Login with credentials
4. App should remember login on restart
5. Test logout from Settings

## Current Status

✅ Android client-side authentication complete
✅ Encrypted token storage implemented
✅ Login/Signup UI with validation
✅ Persistent login across app restarts
✅ Neon Postgres credentials configured
⏳ Backend API server (needs to be created)
⏳ Database schema (needs to be created)
⏳ API endpoint URL (needs to be updated in AuthApiService.kt)

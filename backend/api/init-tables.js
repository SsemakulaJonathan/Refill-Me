const { Pool } = require('pg');

// Neon Postgres connection
const pool = new Pool({
  host: process.env.PGHOST,
  database: process.env.PGDATABASE,
  user: process.env.PGUSER,
  password: process.env.PGPASSWORD,
  port: 5432,
  ssl: {
    rejectUnauthorized: false
  }
});

module.exports = async (req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    const client = await pool.connect();
    
    // Create vehicles table
    await client.query(`
      CREATE TABLE IF NOT EXISTS vehicles (
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
        current_fuel_level REAL DEFAULT 0.0,
        photo_uri TEXT,
        document1_uri TEXT,
        document2_uri TEXT,
        document3_uri TEXT,
        notes TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Add current_fuel_level column if it doesn't exist (for existing databases)
    await client.query(`
      DO $$
      BEGIN
        IF NOT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_name = 'vehicles' AND column_name = 'current_fuel_level'
        ) THEN
          ALTER TABLE vehicles ADD COLUMN current_fuel_level REAL DEFAULT 0.0;
        END IF;
      END $$;
    `);
    
    // Create refills table
    await client.query(`
      CREATE TABLE IF NOT EXISTS refills (
        id SERIAL PRIMARY KEY,
        vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        date TIMESTAMP NOT NULL,
        odometer_reading REAL NOT NULL,
        fuel_amount REAL NOT NULL,
        fuel_before REAL,
        fuel_after REAL,
        price_per_unit REAL NOT NULL,
        total_cost REAL NOT NULL,
        filling_station VARCHAR(255),
        is_full_tank BOOLEAN DEFAULT FALSE,
        notes TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Add fuel_after column if it doesn't exist (for existing databases)
    await client.query(`
      DO $$
      BEGIN
        IF NOT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_name = 'refills' AND column_name = 'fuel_after'
        ) THEN
          ALTER TABLE refills ADD COLUMN fuel_after REAL;
        END IF;
      END $$;
    `);

    // Add fuel_before column if it doesn't exist (for existing databases)
    await client.query(`
      DO $$
      BEGIN
        IF NOT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_name = 'refills' AND column_name = 'fuel_before'
        ) THEN
          ALTER TABLE refills ADD COLUMN fuel_before REAL;
        END IF;
      END $$;
    `);

    // Create vehicle specifications table
    await client.query(`
      CREATE TABLE IF NOT EXISTS vehicle_specifications (
        id SERIAL PRIMARY KEY,
        vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
        name VARCHAR(255) NOT NULL,
        value VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Create services table
    await client.query(`
      CREATE TABLE IF NOT EXISTS services (
        id SERIAL PRIMARY KEY,
        vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        date TIMESTAMP NOT NULL,
        odometer_reading INTEGER NOT NULL,
        service_center VARCHAR(255) NOT NULL,
        total_cost REAL NOT NULL,
        notes TEXT,
        receipt_image_path TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Create service items table
    await client.query(`
      CREATE TABLE IF NOT EXISTS service_items (
        id SERIAL PRIMARY KEY,
        service_id INTEGER NOT NULL REFERENCES services(id) ON DELETE CASCADE,
        service_name VARCHAR(255) NOT NULL,
        cost REAL NOT NULL
      );
    `);

    // Create expenses table
    await client.query(`
      CREATE TABLE IF NOT EXISTS expenses (
        id SERIAL PRIMARY KEY,
        vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        date TIMESTAMP NOT NULL,
        odometer_reading INTEGER NOT NULL,
        vendor VARCHAR(255) NOT NULL,
        total_cost REAL NOT NULL,
        notes TEXT,
        receipt_image_path TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Create expense tasks table
    await client.query(`
      CREATE TABLE IF NOT EXISTS expense_tasks (
        id SERIAL PRIMARY KEY,
        expense_id INTEGER NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
        task_name VARCHAR(255) NOT NULL,
        cost REAL NOT NULL
      );
    `);

    // ── Personal Expenses tables ──────────────────────────────────────────────

    // Create personal expense categories table
    await client.query(`
      CREATE TABLE IF NOT EXISTS personal_expense_categories (
        id SERIAL PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        name VARCHAR(255) NOT NULL,
        color_hex VARCHAR(20) DEFAULT '#9E9E9E',
        is_default BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Create personal expenses table
    await client.query(`
      CREATE TABLE IF NOT EXISTS personal_expenses (
        id SERIAL PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        date TIMESTAMP NOT NULL,
        vendor VARCHAR(255) NOT NULL,
        category_id INTEGER REFERENCES personal_expense_categories(id) ON DELETE SET NULL,
        total_cost REAL NOT NULL,
        notes TEXT,
        receipt_image_path TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // Create personal expense items table
    await client.query(`
      CREATE TABLE IF NOT EXISTS personal_expense_items (
        id SERIAL PRIMARY KEY,
        expense_id INTEGER NOT NULL REFERENCES personal_expenses(id) ON DELETE CASCADE,
        name VARCHAR(255) NOT NULL,
        category_id INTEGER REFERENCES personal_expense_categories(id) ON DELETE SET NULL,
        cost REAL NOT NULL
      );
    `);

    // Create indexes
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_vehicles_user_id ON vehicles(user_id);
      CREATE INDEX IF NOT EXISTS idx_refills_vehicle_id ON refills(vehicle_id);
      CREATE INDEX IF NOT EXISTS idx_refills_user_id ON refills(user_id);
      CREATE INDEX IF NOT EXISTS idx_refills_date ON refills(date);
      CREATE INDEX IF NOT EXISTS idx_specifications_vehicle_id ON vehicle_specifications(vehicle_id);
      CREATE INDEX IF NOT EXISTS idx_services_vehicle_id ON services(vehicle_id);
      CREATE INDEX IF NOT EXISTS idx_services_user_id ON services(user_id);
      CREATE INDEX IF NOT EXISTS idx_services_date ON services(date);
      CREATE INDEX IF NOT EXISTS idx_service_items_service_id ON service_items(service_id);
      CREATE INDEX IF NOT EXISTS idx_expenses_vehicle_id ON expenses(vehicle_id);
      CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses(user_id);
      CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date);
      CREATE INDEX IF NOT EXISTS idx_expense_tasks_expense_id ON expense_tasks(expense_id);
      CREATE INDEX IF NOT EXISTS idx_personal_expense_categories_user_id ON personal_expense_categories(user_id);
      CREATE INDEX IF NOT EXISTS idx_personal_expenses_user_id ON personal_expenses(user_id);
      CREATE INDEX IF NOT EXISTS idx_personal_expenses_date ON personal_expenses(date);
      CREATE INDEX IF NOT EXISTS idx_personal_expense_items_expense_id ON personal_expense_items(expense_id);
    `);

    client.release();

    return res.status(200).json({
      success: true,
      message: 'All database tables initialized successfully',
      tables: ['users', 'vehicles', 'refills', 'vehicle_specifications', 'services', 'service_items', 'expenses', 'expense_tasks', 'personal_expense_categories', 'personal_expenses', 'personal_expense_items']
    });
    
  } catch (error) {
    console.error('Database init error:', error);
    return res.status(500).json({ 
      success: false, 
      message: 'Database initialization failed',
      error: error.message
    });
  }
};

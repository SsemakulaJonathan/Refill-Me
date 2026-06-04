const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
});

module.exports = async (req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ success: false, message: 'Method not allowed' });
  }

  try {
    // Create trips table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS trips (
        id SERIAL PRIMARY KEY,
        vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        start_time BIGINT NOT NULL,
        end_time BIGINT,
        start_latitude DOUBLE PRECISION,
        start_longitude DOUBLE PRECISION,
        end_latitude DOUBLE PRECISION,
        end_longitude DOUBLE PRECISION,
        distance_km DOUBLE PRECISION DEFAULT 0,
        average_speed_kmh DOUBLE PRECISION DEFAULT 0,
        max_speed_kmh DOUBLE PRECISION DEFAULT 0,
        duration_minutes BIGINT DEFAULT 0,
        is_active BOOLEAN DEFAULT true,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
      )
    `);

    // Create index for faster queries
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_trips_vehicle_id ON trips(vehicle_id)
    `);
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_trips_user_id ON trips(user_id)
    `);
    await pool.query(`
      CREATE INDEX IF NOT EXISTS idx_trips_is_active ON trips(is_active)
    `);

    console.log('Trips table created successfully');

    return res.status(200).json({
      success: true,
      message: 'Trips table initialized successfully'
    });
  } catch (error) {
    console.error('Error initializing trips table:', error);
    return res.status(500).json({
      success: false,
      message: 'Failed to initialize trips table',
      error: error.message
    });
  }
};

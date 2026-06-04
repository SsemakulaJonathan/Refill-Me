const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
});

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  
  if (req.method !== 'POST') {
    return res.status(405).json({ success: false, message: 'Method not allowed' });
  }

  try {
    // Add start_address and end_address columns to trips table if they don't exist
    await pool.query(`
      DO $$ 
      BEGIN
        IF NOT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_name = 'trips' AND column_name = 'start_address'
        ) THEN
          ALTER TABLE trips ADD COLUMN start_address TEXT;
        END IF;

        IF NOT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_name = 'trips' AND column_name = 'end_address'
        ) THEN
          ALTER TABLE trips ADD COLUMN end_address TEXT;
        END IF;
      END $$;
    `);

    res.status(200).json({
      success: true,
      message: 'Trip address columns added successfully'
    });
  } catch (error) {
    console.error('Error adding trip address columns:', error);
    res.status(500).json({
      success: false,
      message: 'Error adding columns',
      error: error.message
    });
  }
};

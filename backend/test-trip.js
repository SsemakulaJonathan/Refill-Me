const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
});

async function testTripCreation() {
  try {
    // First, let's check what vehicles exist
    console.log('\n=== Checking vehicles in database ===');
    const vehiclesResult = await pool.query('SELECT id, user_id, make, model FROM vehicles LIMIT 10');
    console.log('Vehicles:', vehiclesResult.rows);
    
    // Check if vehicle 2 exists
    console.log('\n=== Checking if vehicle 2 exists ===');
    const vehicle2 = await pool.query('SELECT * FROM vehicles WHERE id = 2');
    console.log('Vehicle 2:', vehicle2.rows);
    
    // Check trips table
    console.log('\n=== Checking trips table ===');
    const tripsResult = await pool.query('SELECT * FROM trips LIMIT 5');
    console.log('Existing trips:', tripsResult.rows);
    
    await pool.end();
  } catch (error) {
    console.error('Error:', error);
    await pool.end();
  }
}

testTripCreation();

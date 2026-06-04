#!/usr/bin/env node

/**
 * Database Check Script
 * Run with: node check-database.js
 */

require('dotenv').config({ path: './backend/.env' });

const { Pool } = require('pg');

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

async function checkDatabase() {
  const client = await pool.connect();

  try {
    console.log('\n🔍 Checking Refill Me Database...\n');
    console.log('═'.repeat(60));

    // Check tables exist
    const tablesResult = await client.query(`
      SELECT table_name
      FROM information_schema.tables
      WHERE table_schema = 'public'
      ORDER BY table_name;
    `);

    console.log('\n📋 Tables in database:');
    tablesResult.rows.forEach(row => {
      console.log(`   ✓ ${row.table_name}`);
    });

    // Count records in each table
    console.log('\n📊 Record Counts:');

    const users = await client.query('SELECT COUNT(*) FROM users');
    console.log(`   Users: ${users.rows[0].count}`);

    const vehicles = await client.query('SELECT COUNT(*) FROM vehicles');
    console.log(`   Vehicles: ${vehicles.rows[0].count}`);

    const refills = await client.query('SELECT COUNT(*) FROM refills');
    console.log(`   Refills: ${refills.rows[0].count}`);

    const specs = await client.query('SELECT COUNT(*) FROM vehicle_specifications');
    console.log(`   Specifications: ${specs.rows[0].count}`);

    // Show recent data
    console.log('\n👤 Recent Users:');
    const recentUsers = await client.query(
      'SELECT id, email, full_name, created_at FROM users ORDER BY created_at DESC LIMIT 5'
    );
    recentUsers.rows.forEach(user => {
      console.log(`   • ID ${user.id}: ${user.full_name} (${user.email})`);
    });

    console.log('\n🚗 Recent Vehicles:');
    const recentVehicles = await client.query(`
      SELECT v.id, v.name, v.make, v.model, v.year, u.full_name as owner
      FROM vehicles v
      JOIN users u ON v.user_id = u.id
      ORDER BY v.created_at DESC LIMIT 5
    `);
    recentVehicles.rows.forEach(vehicle => {
      console.log(`   • ID ${vehicle.id}: ${vehicle.name} (${vehicle.make} ${vehicle.model} ${vehicle.year}) - Owner: ${vehicle.owner}`);
    });

    console.log('\n⛽ Recent Refills:');
    const recentRefills = await client.query(`
      SELECT r.id, r.date, r.fuel_amount, r.total_cost, v.name as vehicle_name, u.full_name as user_name
      FROM refills r
      JOIN vehicles v ON r.vehicle_id = v.id
      JOIN users u ON r.user_id = u.id
      ORDER BY r.created_at DESC LIMIT 5
    `);
    if (recentRefills.rows.length === 0) {
      console.log('   (No refills yet)');
    } else {
      recentRefills.rows.forEach(refill => {
        const date = new Date(refill.date).toLocaleDateString();
        console.log(`   • ID ${refill.id}: ${refill.vehicle_name} - ${refill.fuel_amount}L - $${refill.total_cost} on ${date}`);
      });
    }

    console.log('\n🔧 Vehicle Specifications:');
    const recentSpecs = await client.query(`
      SELECT s.id, s.name, s.value, v.name as vehicle_name
      FROM vehicle_specifications s
      JOIN vehicles v ON s.vehicle_id = v.id
      ORDER BY s.created_at DESC LIMIT 10
    `);
    if (recentSpecs.rows.length === 0) {
      console.log('   (No specifications yet)');
    } else {
      recentSpecs.rows.forEach(spec => {
        console.log(`   • ${spec.vehicle_name}: ${spec.name} = ${spec.value}`);
      });
    }

    console.log('\n' + '═'.repeat(60));
    console.log('\n✅ Database check complete!\n');

  } catch (error) {
    console.error('\n❌ Error checking database:', error.message);
    console.error(error);
  } finally {
    client.release();
    await pool.end();
  }
}

checkDatabase();

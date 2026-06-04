const pool = require('./db');
const { authenticateToken } = require('./middleware/auth');

// Helper function to update vehicle's current fuel level based on most recent refill by date
async function updateVehicleFuelLevel(vehicleId) {
  try {
    // Get the most recent refill by date (not by creation time)
    // Use COALESCE to fallback to fuel_amount if fuel_after is NULL (for backward compatibility)
    const latestRefill = await pool.query(
      `SELECT COALESCE(fuel_after, fuel_amount) as fuel_level
       FROM refills
       WHERE vehicle_id = $1
       ORDER BY date DESC, created_at DESC
       LIMIT 1`,
      [vehicleId]
    );

    let currentFuelLevel = 0.0;
    if (latestRefill.rows.length > 0) {
      // Use the fuel level from the most recent refill by date
      currentFuelLevel = latestRefill.rows[0].fuel_level;
    }

    // Update the vehicle's current fuel level
    await pool.query(
      'UPDATE vehicles SET current_fuel_level = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
      [currentFuelLevel, vehicleId]
    );

    console.log(`Updated vehicle ${vehicleId} fuel level to ${currentFuelLevel}L`);
  } catch (error) {
    console.error('Error updating vehicle fuel level:', error);
    // Don't throw - we don't want to fail the refill operation if this fails
  }
}

module.exports = async (req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Authenticate user
  const auth = authenticateToken(req);
  if (!auth.success) {
    return res.status(401).json({ success: false, message: auth.message });
  }

  const { userId } = auth;

  try {
    // GET - List all refills (optionally filtered by vehicle)
    if (req.method === 'GET') {
      const { vehicleId } = req.query;
      
      let query = `
        SELECT r.id, r.vehicle_id, r.user_id, r.date, r.odometer_reading, r.fuel_amount,
               r.fuel_after, r.price_per_unit, r.total_cost, r.filling_station, r.is_full_tank,
               r.notes, r.created_at, r.updated_at,
               v.name as vehicle_name, v.make, v.model
        FROM refills r
        JOIN vehicles v ON r.vehicle_id = v.id
        WHERE r.user_id = $1
      `;
      
      const params = [userId];
      
      if (vehicleId) {
        query += ' AND r.vehicle_id = $2';
        params.push(vehicleId);
      }
      
      query += ' ORDER BY r.date DESC, r.created_at DESC';

      const result = await pool.query(query, params);

      return res.status(200).json({
        success: true,
        refills: result.rows
      });
    }

    // POST - Create new refill
    if (req.method === 'POST') {
      const {
        vehicleId, date, odometerReading, fuelAmount, fuelAfter, pricePerUnit,
        totalCost, fillingStation, isFullTank, notes
      } = req.body;

      // Validation
      if (!vehicleId || !date || !odometerReading || !fuelAmount || !pricePerUnit || !totalCost) {
        return res.status(400).json({
          success: false,
          message: 'Vehicle ID, date, odometer reading, fuel amount, price, and total cost are required'
        });
      }

      // Verify vehicle belongs to user
      const vehicleCheck = await pool.query(
        'SELECT id FROM vehicles WHERE id = $1 AND user_id = $2',
        [vehicleId, userId]
      );

      if (vehicleCheck.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Vehicle not found'
        });
      }

      const result = await pool.query(
        `INSERT INTO refills
         (vehicle_id, user_id, date, odometer_reading, fuel_amount, fuel_after, price_per_unit,
          total_cost, filling_station, is_full_tank, notes)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
         RETURNING *`,
        [vehicleId, userId, date, odometerReading, fuelAmount, fuelAfter, pricePerUnit,
         totalCost, fillingStation, isFullTank || false, notes]
      );

      // Update vehicle's current fuel level based on most recent refill by date
      await updateVehicleFuelLevel(vehicleId);

      return res.status(201).json({
        success: true,
        message: 'Refill created successfully',
        refill: result.rows[0]
      });
    }

    // PUT - Update refill
    if (req.method === 'PUT') {
      const { id } = req.query;
      
      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Refill ID is required'
        });
      }

      const {
        vehicleId, date, odometerReading, fuelAmount, fuelAfter, pricePerUnit,
        totalCost, fillingStation, isFullTank, notes
      } = req.body;

      // Check if refill belongs to user
      const checkResult = await pool.query(
        'SELECT vehicle_id FROM refills WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      if (checkResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Refill not found'
        });
      }

      const result = await pool.query(
        `UPDATE refills
         SET vehicle_id = $1, date = $2, odometer_reading = $3, fuel_amount = $4, fuel_after = $5,
             price_per_unit = $6, total_cost = $7, filling_station = $8,
             is_full_tank = $9, notes = $10, updated_at = CURRENT_TIMESTAMP
         WHERE id = $11 AND user_id = $12
         RETURNING *`,
        [vehicleId, date, odometerReading, fuelAmount, fuelAfter, pricePerUnit,
         totalCost, fillingStation, isFullTank || false, notes, id, userId]
      );

      // Update vehicle's current fuel level based on most recent refill by date
      await updateVehicleFuelLevel(vehicleId);

      return res.status(200).json({
        success: true,
        message: 'Refill updated successfully',
        refill: result.rows[0]
      });
    }

    // DELETE - Delete refill
    if (req.method === 'DELETE') {
      const { id } = req.query;
      
      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Refill ID is required'
        });
      }

      // Check if refill belongs to user and get vehicle_id
      const checkResult = await pool.query(
        'SELECT vehicle_id FROM refills WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      if (checkResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Refill not found'
        });
      }

      const vehicleId = checkResult.rows[0].vehicle_id;

      await pool.query(
        'DELETE FROM refills WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      // Update vehicle's current fuel level based on most recent refill by date
      await updateVehicleFuelLevel(vehicleId);

      return res.status(200).json({
        success: true,
        message: 'Refill deleted successfully'
      });
    }

    return res.status(405).json({
      success: false,
      message: 'Method not allowed'
    });

  } catch (error) {
    console.error('Refills API error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
};

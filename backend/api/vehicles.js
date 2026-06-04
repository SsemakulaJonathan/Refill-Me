const pool = require('./db');
const { authenticateToken } = require('./middleware/auth');

// Helper function to update vehicle's current fuel level based on most recent refill by date
async function updateVehicleFuelLevel(vehicleId) {
  try {
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
      currentFuelLevel = latestRefill.rows[0].fuel_level;
    }

    await pool.query(
      'UPDATE vehicles SET current_fuel_level = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
      [currentFuelLevel, vehicleId]
    );
  } catch (error) {
    console.error('Error updating vehicle fuel level:', error);
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
    // GET - List all vehicles for user
    if (req.method === 'GET') {
      const result = await pool.query(
        `SELECT id, user_id, name, make, model, year, license_plate, vin,
                insurance_number, fuel_type, fuel_capacity, current_fuel_level,
                photo_uri, document1_uri, document2_uri, document3_uri, notes,
                created_at, updated_at
         FROM vehicles
         WHERE user_id = $1
         ORDER BY created_at DESC`,
        [userId]
      );

      // Recalculate fuel levels for all vehicles to ensure they're up to date
      for (const vehicle of result.rows) {
        await updateVehicleFuelLevel(vehicle.id);
      }

      // Re-fetch vehicles to get updated fuel levels
      const updatedResult = await pool.query(
        `SELECT id, user_id, name, make, model, year, license_plate, vin,
                insurance_number, fuel_type, fuel_capacity, current_fuel_level,
                photo_uri, document1_uri, document2_uri, document3_uri, notes,
                created_at, updated_at
         FROM vehicles
         WHERE user_id = $1
         ORDER BY created_at DESC`,
        [userId]
      );

      return res.status(200).json({
        success: true,
        vehicles: updatedResult.rows
      });
    }

    // POST - Create new vehicle
    if (req.method === 'POST') {
      const {
        name, make, model, year, licensePlate, vin, insuranceNumber,
        fuelType, fuelCapacity, currentFuelLevel, photoUri, document1Uri, document2Uri,
        document3Uri, notes
      } = req.body;

      // Validation
      if (!name || !fuelCapacity) {
        return res.status(400).json({
          success: false,
          message: 'Vehicle name and fuel capacity are required'
        });
      }

      const result = await pool.query(
        `INSERT INTO vehicles
         (user_id, name, make, model, year, license_plate, vin, insurance_number,
          fuel_type, fuel_capacity, current_fuel_level, photo_uri, document1_uri, document2_uri,
          document3_uri, notes)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
         RETURNING *`,
        [userId, name, make, model, year, licensePlate, vin, insuranceNumber,
         fuelType, fuelCapacity, currentFuelLevel || 0.0, photoUri, document1Uri, document2Uri,
         document3Uri, notes]
      );

      return res.status(201).json({
        success: true,
        message: 'Vehicle created successfully',
        vehicle: result.rows[0]
      });
    }

    // PUT - Update vehicle
    if (req.method === 'PUT') {
      const { id } = req.query;
      
      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Vehicle ID is required'
        });
      }

      const {
        name, make, model, year, licensePlate, vin, insuranceNumber,
        fuelType, fuelCapacity, currentFuelLevel, photoUri, document1Uri, document2Uri,
        document3Uri, notes
      } = req.body;

      // Check if vehicle belongs to user
      const checkResult = await pool.query(
        'SELECT id FROM vehicles WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      if (checkResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Vehicle not found'
        });
      }

      const result = await pool.query(
        `UPDATE vehicles
         SET name = $1, make = $2, model = $3, year = $4, license_plate = $5,
             vin = $6, insurance_number = $7, fuel_type = $8, fuel_capacity = $9,
             current_fuel_level = $10, photo_uri = $11, document1_uri = $12, document2_uri = $13,
             document3_uri = $14, notes = $15, updated_at = CURRENT_TIMESTAMP
         WHERE id = $16 AND user_id = $17
         RETURNING *`,
        [name, make, model, year, licensePlate, vin, insuranceNumber,
         fuelType, fuelCapacity, currentFuelLevel, photoUri, document1Uri, document2Uri,
         document3Uri, notes, id, userId]
      );

      return res.status(200).json({
        success: true,
        message: 'Vehicle updated successfully',
        vehicle: result.rows[0]
      });
    }

    // DELETE - Delete vehicle
    if (req.method === 'DELETE') {
      const { id } = req.query;
      
      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Vehicle ID is required'
        });
      }

      // Check if vehicle belongs to user
      const checkResult = await pool.query(
        'SELECT id FROM vehicles WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      if (checkResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Vehicle not found'
        });
      }

      await pool.query(
        'DELETE FROM vehicles WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      return res.status(200).json({
        success: true,
        message: 'Vehicle deleted successfully'
      });
    }

    return res.status(405).json({
      success: false,
      message: 'Method not allowed'
    });

  } catch (error) {
    console.error('Vehicles API error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
};

const pool = require('./db');
const { authenticateToken } = require('./middleware/auth');

// Helper function to update vehicle's current fuel level based on most recent refill by date
async function updateVehicleFuelLevel(vehicleId) {
  try {
    const latestRefill = await pool.query(
      `SELECT COALESCE(fuel_after, fuel_amount) as fuel_level, date
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

    return {
      vehicleId,
      currentFuelLevel,
      latestRefillDate: latestRefill.rows[0]?.date || null
    };
  } catch (error) {
    console.error('Error updating vehicle fuel level:', error);
    throw error;
  }
}

module.exports = async (req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
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
    // Get all vehicles for the user
    const vehicles = await pool.query(
      'SELECT id FROM vehicles WHERE user_id = $1',
      [userId]
    );

    const results = [];
    for (const vehicle of vehicles.rows) {
      const result = await updateVehicleFuelLevel(vehicle.id);
      results.push(result);
    }

    return res.status(200).json({
      success: true,
      message: `Recalculated fuel levels for ${results.length} vehicles`,
      vehicles: results
    });
  } catch (error) {
    console.error('Recalculate fuel error:', error);
    return res.status(500).json({
      success: false,
      message: 'Failed to recalculate fuel levels',
      error: error.message
    });
  }
};

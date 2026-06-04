const pool = require('./db');
const { authenticateToken } = require('./middleware/auth');

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
    // GET - List all specifications for a vehicle
    if (req.method === 'GET') {
      const { vehicleId } = req.query;

      if (!vehicleId) {
        return res.status(400).json({
          success: false,
          message: 'Vehicle ID is required'
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
        `SELECT id, vehicle_id, name, value, created_at
         FROM vehicle_specifications
         WHERE vehicle_id = $1
         ORDER BY created_at ASC`,
        [vehicleId]
      );

      return res.status(200).json({
        success: true,
        specifications: result.rows
      });
    }

    // POST - Create new specification
    if (req.method === 'POST') {
      const { vehicleId, name, value } = req.body;

      // Validation
      if (!vehicleId || !name || !value) {
        return res.status(400).json({
          success: false,
          message: 'Vehicle ID, name, and value are required'
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
        `INSERT INTO vehicle_specifications (vehicle_id, name, value)
         VALUES ($1, $2, $3)
         RETURNING *`,
        [vehicleId, name, value]
      );

      return res.status(201).json({
        success: true,
        message: 'Specification created successfully',
        specification: result.rows[0]
      });
    }

    // PUT - Update specification
    if (req.method === 'PUT') {
      const { id } = req.query;

      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Specification ID is required'
        });
      }

      const { name, value } = req.body;

      // Check if specification belongs to user's vehicle
      const checkResult = await pool.query(
        `SELECT s.id FROM vehicle_specifications s
         INNER JOIN vehicles v ON s.vehicle_id = v.id
         WHERE s.id = $1 AND v.user_id = $2`,
        [id, userId]
      );

      if (checkResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Specification not found'
        });
      }

      const result = await pool.query(
        `UPDATE vehicle_specifications
         SET name = $1, value = $2
         WHERE id = $3
         RETURNING *`,
        [name, value, id]
      );

      return res.status(200).json({
        success: true,
        message: 'Specification updated successfully',
        specification: result.rows[0]
      });
    }

    // DELETE - Delete specification
    if (req.method === 'DELETE') {
      const { id } = req.query;

      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Specification ID is required'
        });
      }

      // Check if specification belongs to user's vehicle
      const checkResult = await pool.query(
        `SELECT s.id FROM vehicle_specifications s
         INNER JOIN vehicles v ON s.vehicle_id = v.id
         WHERE s.id = $1 AND v.user_id = $2`,
        [id, userId]
      );

      if (checkResult.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Specification not found'
        });
      }

      await pool.query(
        'DELETE FROM vehicle_specifications WHERE id = $1',
        [id]
      );

      return res.status(200).json({
        success: true,
        message: 'Specification deleted successfully'
      });
    }

    return res.status(405).json({
      success: false,
      message: 'Method not allowed'
    });

  } catch (error) {
    console.error('Specifications API error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
};

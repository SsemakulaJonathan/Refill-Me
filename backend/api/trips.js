const { Pool } = require('pg');
const { authenticateToken } = require('./middleware/auth');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
});

module.exports = async (req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Authenticate
  const authResult = authenticateToken(req);
  if (!authResult.success) {
    return res.status(401).json({ success: false, message: authResult.message });
  }

  const userId = authResult.userId;

  try {
    if (req.method === 'GET') {
      const { vehicleId, id } = req.query;

      if (id) {
        // Get single trip
        const result = await pool.query(
          'SELECT * FROM trips WHERE id = $1 AND user_id = $2',
          [id, userId]
        );

        if (result.rows.length === 0) {
          return res.status(404).json({ success: false, message: 'Trip not found' });
        }

        return res.status(200).json({
          success: true,
          trip: result.rows[0]
        });
      } else if (vehicleId) {
        // Get trips for specific vehicle
        const result = await pool.query(
          'SELECT * FROM trips WHERE vehicle_id = $1 AND user_id = $2 ORDER BY start_time DESC',
          [vehicleId, userId]
        );

        return res.status(200).json({
          success: true,
          trips: result.rows
        });
      } else {
        // Get all trips for user
        const result = await pool.query(
          'SELECT * FROM trips WHERE user_id = $1 ORDER BY start_time DESC',
          [userId]
        );

        return res.status(200).json({
          success: true,
          trips: result.rows
        });
      }
    } else if (req.method === 'POST') {
      console.log('POST /api/trips - Request body:', req.body);
      console.log('POST /api/trips - UserId:', userId);
      
      const {
        vehicleId,
        startTime,
        startLatitude,
        startLongitude,
        startAddress,
        isActive = true
      } = req.body;

      console.log('POST /api/trips - Parsed: vehicleId=', vehicleId, 'startTime=', startTime);

      if (!vehicleId || !startTime) {
        console.log('POST /api/trips - Missing required fields');
        return res.status(400).json({
          success: false,
          message: 'vehicleId and startTime are required'
        });
      }

      // Verify vehicle belongs to user
      console.log('POST /api/trips - Checking vehicle ownership: vehicleId=', vehicleId, 'userId=', userId);
      const vehicleCheck = await pool.query(
        'SELECT id FROM vehicles WHERE id = $1 AND user_id = $2',
        [vehicleId, userId]
      );

      console.log('POST /api/trips - Vehicle check result:', vehicleCheck.rows);

      if (vehicleCheck.rows.length === 0) {
        console.log('POST /api/trips - Vehicle not found for user');
        return res.status(404).json({
          success: false,
          message: 'Vehicle not found'
        });
      }

      const result = await pool.query(
        `INSERT INTO trips (
          vehicle_id, user_id, start_time, start_latitude, start_longitude, start_address, is_active
        ) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
        [vehicleId, userId, startTime, startLatitude, startLongitude, startAddress, isActive]
      );

      return res.status(201).json({
        success: true,
        message: 'Trip created successfully',
        trip: result.rows[0]
      });
    } else if (req.method === 'PUT') {
      const { id } = req.query;
      const {
        startLatitude,
        startLongitude,
        startAddress,
        endTime,
        endLatitude,
        endLongitude,
        endAddress,
        distanceKm,
        averageSpeedKmh,
        maxSpeedKmh,
        durationMinutes,
        isActive
      } = req.body;

      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Trip id is required'
        });
      }

      // Verify trip belongs to user
      const tripCheck = await pool.query(
        'SELECT id FROM trips WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      if (tripCheck.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Trip not found'
        });
      }

      const result = await pool.query(
        `UPDATE trips SET
          start_latitude = COALESCE($1, start_latitude),
          start_longitude = COALESCE($2, start_longitude),
          start_address = COALESCE($3, start_address),
          end_time = COALESCE($4, end_time),
          end_latitude = COALESCE($5, end_latitude),
          end_longitude = COALESCE($6, end_longitude),
          end_address = COALESCE($7, end_address),
          distance_km = $8,
          average_speed_kmh = $9,
          max_speed_kmh = $10,
          duration_minutes = $11,
          is_active = $12,
          updated_at = NOW()
        WHERE id = $13 AND user_id = $14
        RETURNING *`,
        [startLatitude, startLongitude, startAddress, endTime, endLatitude, endLongitude, endAddress, distanceKm, averageSpeedKmh, maxSpeedKmh, durationMinutes, isActive, id, userId]
      );

      return res.status(200).json({
        success: true,
        message: 'Trip updated successfully',
        trip: result.rows[0]
      });
    } else if (req.method === 'DELETE') {
      const { id } = req.query;

      if (!id) {
        return res.status(400).json({
          success: false,
          message: 'Trip id is required'
        });
      }

      // Verify trip belongs to user
      const tripCheck = await pool.query(
        'SELECT id FROM trips WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      if (tripCheck.rows.length === 0) {
        return res.status(404).json({
          success: false,
          message: 'Trip not found'
        });
      }

      await pool.query(
        'DELETE FROM trips WHERE id = $1 AND user_id = $2',
        [id, userId]
      );

      return res.status(200).json({
        success: true,
        message: 'Trip deleted successfully'
      });
    } else {
      return res.status(405).json({
        success: false,
        message: 'Method not allowed'
      });
    }
  } catch (error) {
    console.error('Trip API error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
};

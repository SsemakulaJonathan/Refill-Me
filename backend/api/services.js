const { Pool } = require('pg');
const { authenticateToken } = require('./middleware/auth');

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
    const client = await pool.connect();

    // GET - Fetch all services or services by vehicle
    if (req.method === 'GET') {
      const { vehicle_id } = req.query;

      let query, values;
      if (vehicle_id) {
        query = `
          SELECT s.*,
                 json_agg(
                   json_build_object(
                     'id', si.id,
                     'service_name', si.service_name,
                     'cost', si.cost
                   )
                 ) FILTER (WHERE si.id IS NOT NULL) as service_items
          FROM services s
          LEFT JOIN service_items si ON s.id = si.service_id
          WHERE s.vehicle_id = $1 AND s.user_id = $2
          GROUP BY s.id
          ORDER BY s.date DESC
        `;
        values = [vehicle_id, userId];
      } else {
        query = `
          SELECT s.*,
                 json_agg(
                   json_build_object(
                     'id', si.id,
                     'service_name', si.service_name,
                     'cost', si.cost
                   )
                 ) FILTER (WHERE si.id IS NOT NULL) as service_items
          FROM services s
          LEFT JOIN service_items si ON s.id = si.service_id
          WHERE s.user_id = $1
          GROUP BY s.id
          ORDER BY s.date DESC
        `;
        values = [userId];
      }

      const result = await client.query(query, values);
      client.release();
      return res.status(200).json(result.rows);
    }

    // POST - Create new service
    if (req.method === 'POST') {
      const { vehicle_id, user_id, date, odometer_reading, service_center, total_cost, notes, receipt_image_path, service_items } = req.body;

      // Insert service
      const serviceResult = await client.query(
        `INSERT INTO services (vehicle_id, user_id, date, odometer_reading, service_center, total_cost, notes, receipt_image_path)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
         RETURNING *`,
        [vehicle_id, user_id, date, odometer_reading, service_center, total_cost, notes, receipt_image_path]
      );

      const serviceId = serviceResult.rows[0].id;

      // Insert service items if provided
      if (service_items && service_items.length > 0) {
        for (const item of service_items) {
          await client.query(
            `INSERT INTO service_items (service_id, service_name, cost)
             VALUES ($1, $2, $3)`,
            [serviceId, item.service_name, item.cost]
          );
        }
      }

      // Fetch the complete service with items
      const completeService = await client.query(
        `SELECT s.*,
                json_agg(
                  json_build_object(
                    'id', si.id,
                    'service_name', si.service_name,
                    'cost', si.cost
                  )
                ) FILTER (WHERE si.id IS NOT NULL) as service_items
         FROM services s
         LEFT JOIN service_items si ON s.id = si.service_id
         WHERE s.id = $1
         GROUP BY s.id`,
        [serviceId]
      );

      client.release();
      return res.status(201).json(completeService.rows[0]);
    }

    // PUT - Update service
    if (req.method === 'PUT') {
      const { id, vehicle_id, user_id, date, odometer_reading, service_center, total_cost, notes, receipt_image_path, service_items } = req.body;

      // Update service
      await client.query(
        `UPDATE services
         SET vehicle_id = $1, user_id = $2, date = $3, odometer_reading = $4,
             service_center = $5, total_cost = $6, notes = $7, receipt_image_path = $8,
             updated_at = CURRENT_TIMESTAMP
         WHERE id = $9`,
        [vehicle_id, user_id, date, odometer_reading, service_center, total_cost, notes, receipt_image_path, id]
      );

      // Delete existing service items
      await client.query('DELETE FROM service_items WHERE service_id = $1', [id]);

      // Insert new service items
      if (service_items && service_items.length > 0) {
        for (const item of service_items) {
          await client.query(
            `INSERT INTO service_items (service_id, service_name, cost)
             VALUES ($1, $2, $3)`,
            [id, item.service_name, item.cost]
          );
        }
      }

      // Fetch the updated service with items
      const updatedService = await client.query(
        `SELECT s.*,
                json_agg(
                  json_build_object(
                    'id', si.id,
                    'service_name', si.service_name,
                    'cost', si.cost
                  )
                ) FILTER (WHERE si.id IS NOT NULL) as service_items
         FROM services s
         LEFT JOIN service_items si ON s.id = si.service_id
         WHERE s.id = $1
         GROUP BY s.id`,
        [id]
      );

      client.release();
      return res.status(200).json(updatedService.rows[0]);
    }

    // DELETE - Delete service
    if (req.method === 'DELETE') {
      const { id } = req.query;

      await client.query('DELETE FROM services WHERE id = $1', [id]);
      client.release();
      return res.status(200).json({ success: true, message: 'Service deleted successfully' });
    }

    client.release();
    return res.status(405).json({ error: 'Method not allowed' });

  } catch (error) {
    console.error('Services API error:', error);
    return res.status(500).json({
      success: false,
      error: error.message,
      code: error.code
    });
  }
};

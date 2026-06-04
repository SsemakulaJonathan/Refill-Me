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

    // GET - Fetch all expenses or expenses by vehicle
    if (req.method === 'GET') {
      const { vehicle_id } = req.query;

      let query, values;
      if (vehicle_id) {
        query = `
          SELECT e.*,
                 json_agg(
                   json_build_object(
                     'id', et.id,
                     'task_name', et.task_name,
                     'cost', et.cost
                   )
                 ) FILTER (WHERE et.id IS NOT NULL) as expense_tasks
          FROM expenses e
          LEFT JOIN expense_tasks et ON e.id = et.expense_id
          WHERE e.vehicle_id = $1 AND e.user_id = $2
          GROUP BY e.id
          ORDER BY e.date DESC
        `;
        values = [vehicle_id, userId];
      } else {
        query = `
          SELECT e.*,
                 json_agg(
                   json_build_object(
                     'id', et.id,
                     'task_name', et.task_name,
                     'cost', et.cost
                   )
                 ) FILTER (WHERE et.id IS NOT NULL) as expense_tasks
          FROM expenses e
          LEFT JOIN expense_tasks et ON e.id = et.expense_id
          WHERE e.user_id = $1
          GROUP BY e.id
          ORDER BY e.date DESC
        `;
        values = [userId];
      }

      const result = await client.query(query, values);
      client.release();
      return res.status(200).json(result.rows);
    }

    // POST - Create new expense
    if (req.method === 'POST') {
      const { vehicle_id, user_id, date, odometer_reading, vendor, total_cost, notes, receipt_image_path, expense_tasks } = req.body;

      // Insert expense
      const expenseResult = await client.query(
        `INSERT INTO expenses (vehicle_id, user_id, date, odometer_reading, vendor, total_cost, notes, receipt_image_path)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
         RETURNING *`,
        [vehicle_id, user_id, date, odometer_reading, vendor, total_cost, notes, receipt_image_path]
      );

      const expenseId = expenseResult.rows[0].id;

      // Insert expense tasks if provided
      if (expense_tasks && expense_tasks.length > 0) {
        for (const task of expense_tasks) {
          await client.query(
            `INSERT INTO expense_tasks (expense_id, task_name, cost)
             VALUES ($1, $2, $3)`,
            [expenseId, task.task_name, task.cost]
          );
        }
      }

      // Fetch the complete expense with tasks
      const completeExpense = await client.query(
        `SELECT e.*,
                json_agg(
                  json_build_object(
                    'id', et.id,
                    'task_name', et.task_name,
                    'cost', et.cost
                  )
                ) FILTER (WHERE et.id IS NOT NULL) as expense_tasks
         FROM expenses e
         LEFT JOIN expense_tasks et ON e.id = et.expense_id
         WHERE e.id = $1
         GROUP BY e.id`,
        [expenseId]
      );

      client.release();
      return res.status(201).json(completeExpense.rows[0]);
    }

    // PUT - Update expense
    if (req.method === 'PUT') {
      const { id, vehicle_id, user_id, date, odometer_reading, vendor, total_cost, notes, receipt_image_path, expense_tasks } = req.body;

      // Update expense
      await client.query(
        `UPDATE expenses
         SET vehicle_id = $1, user_id = $2, date = $3, odometer_reading = $4,
             vendor = $5, total_cost = $6, notes = $7, receipt_image_path = $8,
             updated_at = CURRENT_TIMESTAMP
         WHERE id = $9`,
        [vehicle_id, user_id, date, odometer_reading, vendor, total_cost, notes, receipt_image_path, id]
      );

      // Delete existing expense tasks
      await client.query('DELETE FROM expense_tasks WHERE expense_id = $1', [id]);

      // Insert new expense tasks
      if (expense_tasks && expense_tasks.length > 0) {
        for (const task of expense_tasks) {
          await client.query(
            `INSERT INTO expense_tasks (expense_id, task_name, cost)
             VALUES ($1, $2, $3)`,
            [id, task.task_name, task.cost]
          );
        }
      }

      // Fetch the updated expense with tasks
      const updatedExpense = await client.query(
        `SELECT e.*,
                json_agg(
                  json_build_object(
                    'id', et.id,
                    'task_name', et.task_name,
                    'cost', et.cost
                  )
                ) FILTER (WHERE et.id IS NOT NULL) as expense_tasks
         FROM expenses e
         LEFT JOIN expense_tasks et ON e.id = et.expense_id
         WHERE e.id = $1
         GROUP BY e.id`,
        [id]
      );

      client.release();
      return res.status(200).json(updatedExpense.rows[0]);
    }

    // DELETE - Delete expense
    if (req.method === 'DELETE') {
      const { id } = req.query;

      await client.query('DELETE FROM expenses WHERE id = $1', [id]);
      client.release();
      return res.status(200).json({ success: true, message: 'Expense deleted successfully' });
    }

    client.release();
    return res.status(405).json({ error: 'Method not allowed' });

  } catch (error) {
    console.error('Expenses API error:', error);
    return res.status(500).json({
      success: false,
      error: error.message,
      code: error.code
    });
  }
};

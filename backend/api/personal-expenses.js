const { Pool } = require('pg');
const { authenticateToken } = require('./middleware/auth');

const pool = new Pool({
  host: process.env.PGHOST,
  database: process.env.PGDATABASE,
  user: process.env.PGUSER,
  password: process.env.PGPASSWORD,
  port: 5432,
  ssl: { rejectUnauthorized: false }
});

const FULL_EXPENSE_QUERY = `
  SELECT e.*,
    COALESCE(
      json_agg(
        json_build_object(
          'id',          ei.id,
          'expense_id',  ei.expense_id,
          'name',        ei.name,
          'category_id', ei.category_id,
          'cost',        ei.cost
        ) ORDER BY ei.id
      ) FILTER (WHERE ei.id IS NOT NULL),
      '[]'
    ) AS items
  FROM personal_expenses e
  LEFT JOIN personal_expense_items ei ON e.id = ei.expense_id
`;

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();

  const authResult = authenticateToken(req);
  if (!authResult.success) {
    return res.status(401).json({ success: false, message: authResult.message });
  }

  const userId = authResult.userId;

  try {
    const client = await pool.connect();

    // GET – all expenses with their items for this user
    if (req.method === 'GET') {
      const result = await client.query(
        FULL_EXPENSE_QUERY + ' WHERE e.user_id = $1 GROUP BY e.id ORDER BY e.date DESC',
        [userId]
      );
      client.release();
      return res.status(200).json(result.rows);
    }

    // POST – create a new expense with items
    if (req.method === 'POST') {
      const { date, vendor, category_id, total_cost, notes, receipt_image_path, items } = req.body;

      if (!vendor || !date) {
        client.release();
        return res.status(400).json({ success: false, message: 'vendor and date are required' });
      }

      const expResult = await client.query(
        `INSERT INTO personal_expenses
           (user_id, date, vendor, category_id, total_cost, notes, receipt_image_path)
         VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
        [userId, date, vendor, category_id || null, total_cost, notes || null, receipt_image_path || null]
      );
      const expenseId = expResult.rows[0].id;

      if (items && items.length > 0) {
        for (const item of items) {
          await client.query(
            `INSERT INTO personal_expense_items (expense_id, name, category_id, cost)
             VALUES ($1, $2, $3, $4)`,
            [expenseId, item.name, item.category_id || null, item.cost]
          );
        }
      }

      const full = await client.query(
        FULL_EXPENSE_QUERY + ' WHERE e.id = $1 GROUP BY e.id',
        [expenseId]
      );
      client.release();
      return res.status(201).json(full.rows[0]);
    }

    // PUT – update an existing expense and replace its items
    if (req.method === 'PUT') {
      const { id, date, vendor, category_id, total_cost, notes, receipt_image_path, items } = req.body;

      await client.query(
        `UPDATE personal_expenses
         SET date = $1, vendor = $2, category_id = $3, total_cost = $4,
             notes = $5, receipt_image_path = $6, updated_at = CURRENT_TIMESTAMP
         WHERE id = $7 AND user_id = $8`,
        [date, vendor, category_id || null, total_cost, notes || null, receipt_image_path || null, id, userId]
      );

      await client.query('DELETE FROM personal_expense_items WHERE expense_id = $1', [id]);

      if (items && items.length > 0) {
        for (const item of items) {
          await client.query(
            `INSERT INTO personal_expense_items (expense_id, name, category_id, cost)
             VALUES ($1, $2, $3, $4)`,
            [id, item.name, item.category_id || null, item.cost]
          );
        }
      }

      const full = await client.query(
        FULL_EXPENSE_QUERY + ' WHERE e.id = $1 GROUP BY e.id',
        [id]
      );
      client.release();
      return res.status(200).json(full.rows[0]);
    }

    // DELETE – remove an expense (items cascade automatically)
    if (req.method === 'DELETE') {
      const { id } = req.query;
      await client.query(
        'DELETE FROM personal_expenses WHERE id = $1 AND user_id = $2',
        [id, userId]
      );
      client.release();
      return res.status(200).json({ success: true });
    }

    client.release();
    return res.status(405).json({ error: 'Method not allowed' });

  } catch (error) {
    console.error('PersonalExpenses API error:', error);
    return res.status(500).json({ success: false, error: error.message });
  }
};

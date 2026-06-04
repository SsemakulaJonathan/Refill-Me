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

const DEFAULT_CATEGORIES = [
  { name: 'Groceries',       color_hex: '#4CAF50' },
  { name: 'Food & Dining',   color_hex: '#FF9800' },
  { name: 'Housing & Rent',  color_hex: '#2196F3' },
  { name: 'Utilities',       color_hex: '#9C27B0' },
  { name: 'Health',          color_hex: '#F44336' },
  { name: 'Entertainment',   color_hex: '#E91E63' },
  { name: 'Travel',          color_hex: '#00BCD4' },
  { name: 'Clothing',        color_hex: '#FF5722' },
  { name: 'Education',       color_hex: '#607D8B' },
  { name: 'Other',           color_hex: '#9E9E9E' },
];

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();

  const authResult = authenticateToken(req);
  if (!authResult.success) {
    return res.status(401).json({ success: false, message: authResult.message });
  }

  const userId = authResult.userId;

  try {
    const client = await pool.connect();

    // GET – return all categories; seed 10 defaults on first use
    if (req.method === 'GET') {
      const countResult = await client.query(
        'SELECT COUNT(*) FROM personal_expense_categories WHERE user_id = $1',
        [userId]
      );

      if (parseInt(countResult.rows[0].count) === 0) {
        for (const cat of DEFAULT_CATEGORIES) {
          await client.query(
            `INSERT INTO personal_expense_categories (user_id, name, color_hex, is_default)
             VALUES ($1, $2, $3, TRUE)`,
            [userId, cat.name, cat.color_hex]
          );
        }
      }

      const result = await client.query(
        `SELECT * FROM personal_expense_categories
         WHERE user_id = $1
         ORDER BY is_default DESC, name ASC`,
        [userId]
      );
      client.release();
      return res.status(200).json(result.rows);
    }

    // POST – create a custom category
    if (req.method === 'POST') {
      const { name, color_hex } = req.body;
      if (!name) {
        client.release();
        return res.status(400).json({ success: false, message: 'name is required' });
      }

      const result = await client.query(
        `INSERT INTO personal_expense_categories (user_id, name, color_hex, is_default)
         VALUES ($1, $2, $3, FALSE) RETURNING *`,
        [userId, name, color_hex || '#9E9E9E']
      );
      client.release();
      return res.status(201).json(result.rows[0]);
    }

    // DELETE – remove a category by id
    if (req.method === 'DELETE') {
      const { id } = req.query;
      await client.query(
        'DELETE FROM personal_expense_categories WHERE id = $1 AND user_id = $2',
        [id, userId]
      );
      client.release();
      return res.status(200).json({ success: true });
    }

    client.release();
    return res.status(405).json({ error: 'Method not allowed' });

  } catch (error) {
    console.error('PersonalExpenseCategories API error:', error);
    return res.status(500).json({ success: false, error: error.message });
  }
};

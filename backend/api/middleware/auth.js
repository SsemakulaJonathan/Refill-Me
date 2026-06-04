const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key-change-this-in-production';

function authenticateToken(req) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return { success: false, message: 'No token provided', status: 401 };
  }

  const token = authHeader.substring(7);

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    return { success: true, userId: decoded.id, email: decoded.email };
  } catch (error) {
    return { success: false, message: 'Invalid or expired token', status: 401 };
  }
}

module.exports = { authenticateToken };

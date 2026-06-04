module.exports = (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  res.status(200).json({
    name: 'Refill Me API',
    version: '1.0.0',
    endpoints: {
      signup: '/api/auth/signup (POST)',
      login: '/api/auth/login (POST)',
      verify: '/api/auth/verify (GET)'
    },
    status: 'healthy'
  });
};

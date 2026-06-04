module.exports = async (req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'GET') {
    return res.status(405).json({ success: false, message: 'Method not allowed' });
  }

  try {
    const { query, page = 1 } = req.query;

    if (!query) {
      return res.status(400).json({
        success: false,
        message: 'Search query is required'
      });
    }

    // Unsplash API key - using demo access key
    // For production, sign up at https://unsplash.com/developers and add UNSPLASH_ACCESS_KEY to Vercel env vars
    const UNSPLASH_ACCESS_KEY = process.env.UNSPLASH_ACCESS_KEY || 'demo';
    
    const searchQuery = encodeURIComponent(query + ' car vehicle');
    const url = `https://api.unsplash.com/search/photos?query=${searchQuery}&page=${page}&per_page=20&orientation=landscape`;

    const response = await fetch(url, {
      headers: {
        'Authorization': `Client-ID ${UNSPLASH_ACCESS_KEY}`
      }
    });

    if (!response.ok) {
      throw new Error('Unsplash API request failed');
    }

    const data = await response.json();

    // Format results for mobile app
    const photos = data.results.map(photo => ({
      id: photo.id,
      url: photo.urls.regular,
      thumbnail: photo.urls.thumb,
      description: photo.description || photo.alt_description,
      photographer: photo.user.name,
      photographerUrl: photo.user.links.html
    }));

    return res.status(200).json({
      success: true,
      photos,
      total: data.total,
      totalPages: data.total_pages
    });

  } catch (error) {
    console.error('Photo search error:', error);
    return res.status(500).json({
      success: false,
      message: 'Photo search failed',
      error: error.message
    });
  }
};

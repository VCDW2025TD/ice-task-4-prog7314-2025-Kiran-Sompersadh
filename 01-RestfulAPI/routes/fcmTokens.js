// routes/fcmTokens.js
const express = require('express');
const router = express.Router();
const FcmToken = require('../models/FcmToken');

router.post('/register', async (req, res) => {
  try {
    const { userId, token, lat, lng } = req.body;
    if (!userId || !token) return res.status(400).json({ error: 'Missing userId or token' });

    // Save or update token
    await FcmToken.findOneAndUpdate(
      { userId, token },
      { lat, lng },
      { upsert: true, new: true }
    );

    res.json({ success: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to save token' });
  }
});

module.exports = router;

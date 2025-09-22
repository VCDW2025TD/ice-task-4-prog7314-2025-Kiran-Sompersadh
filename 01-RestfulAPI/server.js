// server.js
const admin = require('firebase-admin');
const serviceAccount = require('./firebase-service-account.json');
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const multer = require('multer');
const { GridFSBucket } = require('mongodb');
const Meme = require('./models/Meme'); // your Meme model
const FcmToken = require('./models/FcmToken'); // import at top
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// MongoDB connection
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log('✅ Connected to MongoDB'))
  .catch(err => console.error('❌ MongoDB connection failed:', err.message));

// Multer memory storage
const storage = multer.memoryStorage();
const upload = multer({ storage });

// GridFSBucket (for uploads and serving)
let gfsBucket;
mongoose.connection.once('open', () => {
  gfsBucket = new GridFSBucket(mongoose.connection.db, { bucketName: 'memes' });
  console.log('✅ GridFSBucket ready');
});

// Health check
app.get('/', (req, res) => res.send('🎉 MemeStream API is running with file uploads!'));

// Upload meme image + metadata
app.post('/memes/upload', upload.single('image'), async (req, res) => {
    console.log('📥 Upload request received:', req.body, req.file?.originalname);
  try {
    const { userId, caption, lat, lng } = req.body;
    if (!req.file) return res.status(400).json({ error: 'No file uploaded' });

    // ✅ Wrap GridFS upload in a Promise
    const imageUrl = await new Promise((resolve, reject) => {
      const uploadStream = gfsBucket.openUploadStream(req.file.originalname);
      uploadStream.end(req.file.buffer);

      uploadStream.on('finish', () => {
        resolve(`/memes/file/${uploadStream.id}`);
      });

      uploadStream.on('error', (err) => {
        reject(err);
      });
    });

    // ✅ Save meme metadata after successful upload
    const meme = new Meme({
      userId,
      caption,
      lat: parseFloat(lat),
      lng: parseFloat(lng),
      imageUrl,
      timestamp: new Date()
    });

    const saved = await meme.save();

    res.status(201).json(saved);
    // ✅ Send FCM notification
    sendMemeNotification(saved);

  } catch (err) {
    console.error('❌ Upload failed:', err);
    res.status(500).json({ error: err.message });
  }
});

// Serve uploaded images
app.get('/memes/file/:id', async (req, res) => {
  try {
    const fileId = new mongoose.Types.ObjectId(req.params.id);
    const downloadStream = gfsBucket.openDownloadStream(fileId);

    downloadStream.pipe(res);

    downloadStream.on('error', (err) => {
      console.error(err);
      res.status(404).json({ error: 'File not found' });
    });

  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// Existing /memes routes
app.use('/memes', require('./routes/memes'));

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Server running on http://localhost:${PORT}`);
});

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

app.use('/fcm', require('./routes/fcmTokens'));

async function sendMemeNotification(meme) {
  try {
    // Get all tokens except uploader
    const tokens = await FcmToken.find({ userId: { $ne: meme.userId } });
    if (!tokens.length) return;

    const message = {
      notification: {
        title: "New Meme Nearby!",
        body: meme.caption || "Check out this new meme!"
      },
      data: {
        memeId: meme._id.toString(),
        imageUrl: meme.imageUrl,
        lat: meme.lat?.toString() || "0",
        lng: meme.lng?.toString() || "0"
      },
      tokens: tokens.map(t => t.token)
    };

    const response = await admin.messaging().sendMulticast(message);
    console.log('✅ Notifications sent:', response.successCount);
  } catch (err) {
    console.error('❌ Failed to send notifications:', err);
  }
}
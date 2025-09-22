// models/FcmToken.js
const mongoose = require('mongoose');

const FcmTokenSchema = new mongoose.Schema({
  userId: { type: String, required: true },
  token: { type: String, required: true },
  lat: { type: Number }, // optional, if you want location targeting
  lng: { type: Number }
});

module.exports = mongoose.model('FcmToken', FcmTokenSchema);

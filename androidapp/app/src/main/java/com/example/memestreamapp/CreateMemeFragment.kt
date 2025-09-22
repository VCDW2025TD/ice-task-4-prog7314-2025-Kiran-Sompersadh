package com.example.memestreamapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

private const val TAG = "CreateMemeFragment"

class CreateMemeFragment : Fragment(R.layout.fragment_create_meme) {

    private var selectedImageUri: Uri? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // permission continuation holder for suspend-based permission request
    private var permissionContinuation: ( (Boolean) -> Unit )? = null

    // launcher to request location permissions (fine + coarse)
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            permissionContinuation?.let { cont ->
                cont(granted)
                permissionContinuation = null
            }
        }

    // Launcher for picking an image from the gallery
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                view?.findViewById<ImageView>(R.id.imageViewMeme)?.let { img ->
                    Glide.with(this).load(it).into(img)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val imgSelected = view.findViewById<ImageView>(R.id.imageViewMeme)
        val buttonPickImage = view.findViewById<Button>(R.id.buttonPickImage)
        val etCaption = view.findViewById<EditText>(R.id.editTextCaption)
        val btnPost = view.findViewById<Button>(R.id.buttonUploadMeme)

        buttonPickImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnPost.setOnClickListener {
            val caption = etCaption.text.toString().trim()
            val user = FirebaseAuth.getInstance().currentUser

            if (selectedImageUri == null || caption.isEmpty() || user == null) {
                Toast.makeText(requireContext(), "Select image and enter caption", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 1) Get location
                    val (lat, lng, alt) = getLocationWithPermissions()
                    Log.d(TAG, "Location: lat=$lat, lng=$lng, alt=$alt")

                    // 2) Convert URI -> File
                    val file = uriToTempFile(selectedImageUri!!)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                    // 3) Build RequestBody fields
                    val userIdBody = user.uid.toRequestBody("text/plain".toMediaTypeOrNull())
                    val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
                    val latBody = lat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val lngBody = lng.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    // 4) Try uploading to API
                    val response = ApiService.memeApi.uploadMeme(
                        image = imagePart,
                        userId = userIdBody,
                        caption = captionBody,
                        lat = latBody,
                        lng = lngBody,
                    )

                    launch(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Meme posted!", Toast.LENGTH_SHORT).show()
                            etCaption.text.clear()
                            imgSelected.setImageDrawable(null)
                            selectedImageUri = null
                        } else {
                            // Save offline if server rejects
                            saveMemeOffline(user.uid, caption, selectedImageUri!!, lat, lng)
                            Toast.makeText(requireContext(), "Saved offline (server error)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error uploading meme", e)
                    // Save offline if network/API fails
                    saveMemeOffline(
                        user?.uid ?: "unknown",
                        caption,
                        selectedImageUri!!,
                        0.0,
                        0.0
                    )
                    launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Saved offline (no network)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /** Convert URI -> temp File in cache (safe for Multipart). */
    private fun uriToTempFile(uri: Uri): File {
        val inputStream: InputStream = requireContext().contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open input stream from uri")
        val tempFile = File.createTempFile("upload", ".jpg", requireContext().cacheDir)
        inputStream.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        return tempFile
    }

    private fun saveMemeOffline(
        userId: String,
        caption: String,
        imageUri: Uri,
        lat: Double,
        lng: Double
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert image to a local cache file so we can reload later
                val file = uriToTempFile(imageUri)

                val localEntity = MemeEntity(
                    id = UUID.randomUUID().toString(), // unique temp ID
                    userId = userId,
                    caption = caption,
                    imageUrl = file.absolutePath, // local file path
                    lat = lat,
                    lng = lng,
                    timestamp = System.currentTimeMillis().toString(),
                    isSynced = false
                )

                // Insert into RoomDB
                val db = MemeDatabase.getDatabase(requireContext())
                db.memeDao().insertMeme(localEntity)

                Log.d(TAG, "Saved meme offline: $localEntity")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save meme offline", e)
            }
        }
    }
    /** Suspend helper: ensure permissions then get current location (lat, lng, alt). */
    private suspend fun getLocationWithPermissions(): Triple<Double, Double, Double> {
        // if already granted, proceed
        val hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val ok = hasFine || hasCoarse

        if (!ok) {
            val granted = requestLocationPermissionSuspend()
            if (!granted) {
                Log.w(TAG, "Location permission not granted, returning default 0.0s")
                return Triple(0.0, 0.0, 0.0)
            }
        }

        // Try getCurrentLocation (high accuracy). Fallback to lastLocation if null.
        return suspendCancellableCoroutine { cont ->
            try {
                fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val lat = location.latitude
                            val lng = location.longitude
                            val alt = if (location.hasAltitude()) location.altitude else 0.0
                            cont.resume(Triple(lat, lng, alt))
                        } else {
                            // fallback to lastLocation
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { last ->
                                    if (last != null) {
                                        val lat = last.latitude
                                        val lng = last.longitude
                                        val alt = if (last.hasAltitude()) last.altitude else 0.0
                                        cont.resume(Triple(lat, lng, alt))
                                    } else {
                                        cont.resume(Triple(0.0, 0.0, 0.0))
                                    }
                                }
                                .addOnFailureListener {
                                    cont.resume(Triple(0.0, 0.0, 0.0))
                                }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(Triple(0.0, 0.0, 0.0))
                    }
            } catch (ex: Exception) {
                cont.resume(Triple(0.0, 0.0, 0.0))
            }
        }
    }

    /** Suspends until user responds to permission dialog. */
    private suspend fun requestLocationPermissionSuspend(): Boolean =
        suspendCancellableCoroutine { cont ->
            permissionContinuation = { granted ->
                cont.resume(granted)
            }
            // launch the permission request
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            cont.invokeOnCancellation { permissionContinuation = null }
        }
}

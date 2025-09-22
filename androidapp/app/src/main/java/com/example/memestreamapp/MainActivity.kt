package com.example.memestreamapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get NavController safely
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Hook up bottom nav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // --- Step 5: Get FCM token ---
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "✅ FCM Token: $token")
                sendTokenToServer(token)
            }
    }

    private fun sendTokenToServer(token: String?) {
        if (token == null) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val json = JSONObject().apply {
            put("userId", userId)
            put("token", token)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://meamstreamicetas1api.onrender.com/fcm/register") // replace with your actual server
            .post(requestBody)
            .build()

        // ✅ This is where we use OkHttp Callback
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ Failed to send token to server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "✅ Token sent to server: ${response.code}")
                response.close()
            }
        })
    }
}

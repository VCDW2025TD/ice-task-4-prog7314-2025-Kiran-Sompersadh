package com.example.memestreamapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.example.memestreamapp.model.toApiModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch


class ProfileFragment : Fragment(R.layout.fragment_profile) {

    // Lazy init against a real Context (only called after onAttach)
    private val prefs by lazy {
        requireContext().getSharedPreferences(
            "memestream_prefs",
            Context.MODE_PRIVATE
        )
    }

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Wire up views via the view passed in
        val nameView      = view.findViewById<TextView>(R.id.txtName)
        val emailView     = view.findViewById<TextView>(R.id.txtEmail)
        val profileImage  = view.findViewById<ImageView>(R.id.imgProfile)
        val signOutButton = view.findViewById<Button>(R.id.btnSignOut)

        updateUI(currentUser, nameView, emailView, profileImage)

        signOutButton.setOnClickListener {
            signOut()
        }

        currentUser?.let {
            loadUserMemes(it.uid)
        }

        val db = Room.databaseBuilder(
            requireContext(),
            MemeDatabase::class.java,
            "memestream-db"
        ).build()
        val repository = MemeRepository(db.memeDao())
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val memeEntities = repository.getUserMemes(userId)
            val memes = memeEntities.map { it.toApiModel() }  // <-- convert to List<Meme>

            val adapter = UserMemesAdapter(memes) { meme ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${meme.caption}\n${meme.imageUrl}")
                }
                startActivity(Intent.createChooser(shareIntent, "Share meme via"))
            }
            view.findViewById<RecyclerView>(R.id.rvUserMemes).adapter = adapter
        }
    }

    private fun updateUI(
        user: FirebaseUser?,
        nameView: TextView,
        emailView: TextView,
        profileImage: ImageView
    ) {
        if (user != null) {
            nameView.text  = user.displayName ?: "Unknown User"
            emailView.text = user.email       ?: ""

            user.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(profileImage)
            }
        } else {
            nameView.text  = "Guest"
            emailView.text = ""
            profileImage.setImageResource(R.drawable.ic_person)
        }
    }

    private fun signOut() {
        // Firebase sign-out
        auth.signOut()

        // Google sign-out (needs an Activity/Context)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        GoogleSignIn
            .getClient(requireActivity(), gso)
            .signOut()
            .addOnCompleteListener {
                // Clear any saved prefs
                prefs.edit().putBoolean("biometric_enabled", false).apply()

                // Launch login screen and finish host Activity
                startActivity(
                    Intent(requireActivity(), LoginActivity::class.java)
                )
                requireActivity().finish()
            }
    }

    private fun loadUserMemes(userId: String) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.rvUserMemes)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val memes = ApiService.memeApi.getMemesByUser(userId) // GET /memes?userId=...
                val adapter = UserMemesAdapter(memes) { meme ->
                    shareMeme(meme)
                }
                recyclerView?.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load memes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareMeme(meme: Meme) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${meme.caption}\n${meme.imageUrl}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Meme via"))
    }
}
package com.example.memestreamapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserMemesAdapter(
    private val memes: List<Meme>,
    private val onShareClicked: (Meme) -> Unit
) : RecyclerView.Adapter<UserMemesAdapter.MemeViewHolder>() {

    inner class MemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMeme: ImageView = view.findViewById(R.id.imgMeme)
        val txtCaption: TextView = view.findViewById(R.id.txtCaption)
        val btnShare: Button = view.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_meme, parent, false)
        return MemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemeViewHolder, position: Int) {
        val meme = memes[position]
        holder.txtCaption.text = meme.caption

        // ✅ Prepend full server URL
        val fullUrl = "https://meamstreamicetas1api.onrender.com${meme.imageUrl}"

        Glide.with(holder.itemView)
            .load(fullUrl)
            .placeholder(R.drawable.ic_person) // optional placeholder
            .error(R.drawable.ic_person)       // fallback if image fails
            .into(holder.imgMeme)

        holder.btnShare.setOnClickListener { onShareClicked(meme) }
    }


    override fun getItemCount(): Int = memes.size
}
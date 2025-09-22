package com.example.memestreamapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val _memes = MutableLiveData<List<String>>()
    val memes: LiveData<List<String>> = _memes

    private val apiKey = "yoBrgMLmoMJmRqAN9CPgxOdpWh4prkav" // replace with your GIPHY key

    fun loadTrending() {
        viewModelScope.launch {
            try {
                val response = GiphyService.api.getTrending(apiKey)
                val urls = response.data.map { it.images.original.url }
                _memes.postValue(urls)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
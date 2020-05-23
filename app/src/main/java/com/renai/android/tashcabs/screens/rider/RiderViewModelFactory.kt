package com.renai.android.tashcabs.screens.rider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.renai.android.tashcabs.network.NetworkService

@Suppress("UNCHECKED_CAST") class RiderViewModelFactory(private val networkService: NetworkService) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RiderViewModel::class.java)) {
            return RiderViewModel(networkService) as T
        } else {
            throw Exception("Unknown ViewModel class")
        }
    }
}
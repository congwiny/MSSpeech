package com.msspeech.demo.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

object AsrViewModelFactory {

    private const val useXunFei = false

    fun getAsrViewModel(owner: ViewModelStoreOwner): AsrViewModel {
        return if (useXunFei) {
            ViewModelProvider(owner)[XFAsrViewModel::class.java]
        } else {
            ViewModelProvider(owner)[MSAsrViewModel::class.java]
        }
    }

}
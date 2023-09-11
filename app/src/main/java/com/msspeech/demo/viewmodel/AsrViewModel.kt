package com.msspeech.demo.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class AsrViewModel : ViewModel() {
    open val mBuffer: ByteArray = ByteArray(182)
    open val asrInitResult = MutableLiveData<Boolean?>(null)
    open val asrWaveData = MutableLiveData<ByteArray?>()
    open val asrStartEvent = MutableLiveData<Boolean>(false)
    open val asrResultData = MutableLiveData<AsrResultData>()
    open var isSDKInited = false
        protected set
    open var isInitingSDK = false
        protected set
    open var asrMode: Int = ASR_MODE_SINGLE_SENTENCE


    abstract fun releaseAsr()

    abstract fun startDialog(callback: ((code: Int) -> Unit)?)

    abstract fun stopDialog(callback: ((code: Long) -> Unit)?)


    abstract  fun initAsrSDK()

    companion object {
        const val TAG = "AsrViewModel"
        const val ASR_MODE_SINGLE_SENTENCE = 1
        const val ASR_MODE_MULTI_SENTENCE = 2

        const val EVENT_ASR_PARTIAL_RESULT = 1
        const val EVENT_SENTENCE_START = 2
        const val EVENT_SENTENCE_END = 3
    }
}

class AsrResultData(val event: Int, val asrText: String)
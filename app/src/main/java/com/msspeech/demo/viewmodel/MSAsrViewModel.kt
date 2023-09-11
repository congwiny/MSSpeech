package com.msspeech.demo.viewmodel

import androidx.lifecycle.viewModelScope
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.msspeech.demo.utils.Logger
import com.msspeech.msspeech.MicrophoneStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MSAsrViewModel : AsrViewModel() {
    private val mLogger = Logger(javaClass)

    private val speechKey = "b301e554220b49ec8b658e0a9e6f7ba2"
    private val speechRegion = "eastasia"

    private var isStart = false

    private val speechConfig: SpeechConfig by lazy {
        SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
            speechRecognitionLanguage = "zh-CN"
        }
    }

    private var recognizer: SpeechRecognizer? = null;

    override fun initAsrSDK() {
        if (!isSDKInited && !isInitingSDK) {
            isInitingSDK = true
            isStart = false
            recognizer = createSpeechRecognizer()
            asrInitResult.postValue(true)
            isInitingSDK = false
            isSDKInited = true
        }
    }

    override fun releaseAsr() {
        if (!isSDKInited) {
            return
        }
        try {
            recognizer?.close()
            microphoneStream?.close()
            isSDKInited = false
            recognizer = null
        } catch (e: Exception) {
            mLogger.ci("releaseAsr", e)
        }
    }


    private var content = ""

    private var microphoneStream: MicrophoneStream? = null
    private fun createMicrophoneStream(): MicrophoneStream? {
        releaseMicrophoneStream()
        microphoneStream = MicrophoneStream()
        return microphoneStream
    }

    private fun releaseMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream!!.close()
            microphoneStream = null
        }
    }

    private fun createSpeechRecognizer(): SpeechRecognizer {
        val audioInput: AudioConfig = AudioConfig.fromStreamInput(createMicrophoneStream())

        return SpeechRecognizer(speechConfig,audioInput).apply {
            speechEndDetected.addEventListener { _, _ ->
                mLogger.ci("speechEndDetected")

            }
            speechStartDetected.addEventListener { _, _ ->
                mLogger.ci("speechStartDetected")
                content = ""
                asrResultData.postValue(AsrResultData(EVENT_SENTENCE_START, ""))
                asrStartEvent.postValue(true)
            }
            recognizing.addEventListener { s, e ->
                mLogger.ci("recognizing ${e.result.text}")
                if (e.result.reason == ResultReason.RecognizingSpeech) {
                    content = e.result.text
                    asrResultData.postValue(AsrResultData(EVENT_ASR_PARTIAL_RESULT, e.result.text))
                }
            }
            recognized.addEventListener { s, e ->
                mLogger.ci("recognized")
                if (e.result.reason == ResultReason.RecognizedSpeech) {
                    content = e.result.text
                }
                asrStartEvent.postValue(false)
                asrResultData.postValue(AsrResultData(EVENT_SENTENCE_END, content))
            }
            canceled.addEventListener { _, _ ->
                mLogger.ci("canceled")
            }
            sessionStarted.addEventListener { _, _ ->
                content =""
                mLogger.ci("sessionStarted")
            }
            sessionStopped.addEventListener { _, _ ->
                mLogger.ci("sessionStopped")

            }
        }
    }

    override fun startDialog(callback: ((code: Int) -> Unit)?) {
        if (!isSDKInited) {
            callback?.invoke(-1)
            return
        }
        viewModelScope.launch {
            if(isStart){
                withContext(Dispatchers.IO){
                    microphoneStream?.resume()
                    recognizer?.recognizeOnceAsync()
                }
            }else{
                withContext(Dispatchers.IO) {
                    microphoneStream?.start()
                    recognizer?.recognizeOnceAsync()
                }
                isStart = true
            }
            callback?.invoke(0)
        }

    }

    override fun stopDialog(callback: ((code: Long) -> Unit)?) {
        if (!isSDKInited) {
            callback?.invoke(-1)
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                microphoneStream?.pause()
            }
            callback?.invoke(0)
        }
    }

}
package com.msspeech.demo.msspeech

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel
import com.microsoft.cognitiveservices.speech.KeywordRecognizer
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.msspeech.demo.App
import com.msspeech.demo.utils.Logger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MsSpeechViewModel : ViewModel() {
    private val mLogger = Logger(javaClass)
    val isInitialized = MutableLiveData<Boolean>()
    val speechResult = MutableLiveData<String>()
    val awakeKeyword = MutableLiveData<String?>(null)
    val isRecognizing = MutableLiveData(false)

    private val speechKey = "b301e554220b49ec8b658e0a9e6f7ba2"
    private val speechRegion = "eastasia"

    private val speechConfig: SpeechConfig by lazy {
        SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
            speechRecognitionLanguage = "zh-CN"
        }
    }

    private val recognizer: SpeechRecognizer by lazy {
        SpeechRecognizer(speechConfig).apply {
            speechEndDetected.addEventListener { _, _ ->
                mLogger.ci("speechEndDetected")
            }
            speechStartDetected.addEventListener { _, _ ->
                mLogger.ci("speechStartDetected")
                isRecognizing.postValue(true)
            }
            recognizing.addEventListener { s, e ->
                mLogger.ci("recognizing ${e.result.text}")
                if (e.result.reason == ResultReason.RecognizingSpeech) {
                    speechResult.postValue(e.result.text)
                }
            }
            recognized.addEventListener { _, _ ->
                mLogger.ci("recognized")
            }
            canceled.addEventListener { _, _ ->
                mLogger.ci("canceled")
            }
            sessionStarted.addEventListener { _, _ ->
                mLogger.ci("sessionStarted")
            }
            sessionStopped.addEventListener { _, _ ->
                mLogger.ci("sessionStopped")
                isRecognizing.postValue(false)
            }
        }
    }


    private lateinit var keywordModel: KeywordRecognitionModel

    private val keywordRecognizer by lazy {
        KeywordRecognizer(AudioConfig.fromDefaultMicrophoneInput()).apply {
            recognized.addEventListener { s, e ->
                mLogger.ci("keyword recognized, $s $e")
                awakeKeyword.postValue(e.result.text)
            }
            canceled.addEventListener { s, e ->
                mLogger.ci("keyword canceled $s $e")
            }
        }
    }

    fun initialize() {
        viewModelScope.launch(Dispatchers.IO) {
            keywordModel = KeywordRecognitionModel.fromStream(
                App.CONTEXT.assets.open("msspeech/hello_kexin.table"),
                "可心你好",
                false
            )
            isInitialized.postValue(true)
        }
    }

    fun startSpeechOnce() {
        keywordRecognizer.stopRecognitionAsync()
        keywordRecognizer.close()
        viewModelScope.launch {
            delay(500)
            recognizer.recognizeOnceAsync()
        }
    }

    fun startKeywordRecognize() {
        isRecognizing.value!! && return
        isRecognizing.value = true
        keywordRecognizer.recognizeOnceAsync(keywordModel)
    }
}
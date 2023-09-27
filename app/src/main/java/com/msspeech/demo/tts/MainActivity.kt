//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//
// <code>
package com.msspeech.demo.tts

import android.Manifest.permission
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArrayMap
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.msspeech.demo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var speechConfig: SpeechConfig? = null
    private var synthesizer: SpeechSynthesizer? = null

    private val ttsText = arrayOf(
        "作为老周发言人",
        "作为老周发言人",
        "作为老周发言人",
        "作为老周发言人",
        "作为老周发言人",
        "嗨",
        "很简单",
        "作为老周发言人",
        "作为老周发言人",
        "作为老周发言人",
    )

    @Volatile
    private var currentText: String? = null

    private val map = ArrayMap<String, TTSResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tts_activity_main)

        // Note: we need to request the permissions
        val requestCode = 5 // unique code for the permission request
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(permission.INTERNET),
            requestCode
        )

        // Initialize speech synthesizer and its dependencies
        speechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion).apply {
            setSpeechSynthesisLanguage("zh-CN") //语言
            setSpeechSynthesisVoiceName("zh-CN-XiaoxiaoNeural") //语言名称
        }
        assert(speechConfig != null)
        synthesizer = SpeechSynthesizer(speechConfig).apply {
            SynthesisStarted.addEventListener { sender, e ->
                Log.e(
                    "MainActivity",
                    "SynthesisStarted sender=" + sender + ",resultId=" + e.result.resultId + ",size=" + e.result.audioData.size
                )
                val ttsResult = TTSResult()
                ttsResult.startTime = System.currentTimeMillis()
                ttsResult.text = currentText
                map[e.result.resultId] = ttsResult
            }
            SynthesisCompleted.addEventListener { sender, e ->
                Log.e(
                    "MainActivity",
                    "SynthesisCompleted sender=" + sender + ",e=" + e.result.resultId
                )

            }
            Synthesizing.addEventListener { sender, e ->
                Log.e(
                    "MainActivity",
                    "Synthesizing sender=" + sender + ",resultId=" + e.result.resultId + ",size=" + e.result.audioData.size
                )

                val ttsResult = map[e.result.resultId]
                if (ttsResult != null && ttsResult.seconds < 0f) {
                    ttsResult.seconds =
                        (System.currentTimeMillis() - ttsResult.startTime) * 1f / 1000
                }
            }
            SynthesisCanceled.addEventListener { sender, e ->
                Log.e(
                    "MainActivity",
                    "SynthesisCanceled sender=" + sender + ",e=" + e.result.resultId
                )
            }
        }
        assert(synthesizer != null)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release speech synthesizer and its dependencies
        synthesizer!!.close()
        speechConfig!!.close()
    }

    fun onSpeechButtonClicked(v: View?) {
        lifecycleScope.launch {
            ttsText.forEach {
                speakText(it)
            }
          //  speakText("据报道，目前停靠在中美洲岛国安提瓜和巴布达一处港口的豪华游艇“阿尔法·内罗”被认为属于俄罗斯富豪安德烈・古列耶夫，后者遭到美国制裁。维护这一价值1.2亿美元、几乎达到足球场长度的游艇每周需要支付2.8万美元，包括一名意大利船长的工资以及每天2000美元的柴油费用于空调运行。如果关闭空调，霉菌会在48小时内扩散，破坏船体内部以及一些画作。")
            Log.e("MainActivity", "TTS finished map=" + ArrayList(map.values.sortedBy { it.startTime }))
        }

    }

    private suspend fun speakText(text: String) {
        val outputMessage = findViewById<TextView>(R.id.outputMessage)
        withContext(Dispatchers.IO) {
            try {
                currentText = text
                // Note: this will block the UI thread, so eventually, you want to register for the event
                val result = synthesizer!!.SpeakText(text)!!
                if (result.reason == ResultReason.SynthesizingAudioCompleted) {
                    outputMessage.text = "Speech synthesis succeeded."
                } else if (result.reason == ResultReason.Canceled) {
                    val cancellationDetails =
                        SpeechSynthesisCancellationDetails.fromResult(result).toString()
                    outputMessage.text = "Error synthesizing. Error detail: " +
                            System.lineSeparator() + cancellationDetails +
                            System.lineSeparator() + "Did you update the subscription info?"
                }
                result.close()
            } catch (ex: Exception) {
                Log.e("SpeechSDKDemo", "unexpected " + ex.message)
                assert(false)
            }
        }
    }

    companion object {
        // Replace below with your own subscription key
        private const val speechSubscriptionKey = "b301e554220b49ec8b658e0a9e6f7ba2"

        // Replace below with your own service region (e.g., "westus").
        private const val serviceRegion = "eastasia"
    }
}

class TTSResult {
    @Volatile
    var text: String? = null
    @Volatile
    var startTime: Long = 0
    @Volatile
    var seconds: Float = -1f
    override fun toString(): String {
        return "TTSResult(text=$text, startTime=$startTime, seconds=$seconds)"
    }


}
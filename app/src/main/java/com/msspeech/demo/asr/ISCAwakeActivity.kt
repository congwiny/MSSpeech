package com.msspeech.demo.asr

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope

import com.qihoo.superbrain.voicewake2.media.audio.RecorderCallback
import com.qihoo.superbrain.voicewake2.tool.calculateVolume
import com.qihoo.zhinao.demo.asr.IvwActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable


/**
 * @Desc: ivw唤醒
 * @Author leon
 * @Date 2023/2/23-17:14
 * Copyright 2023 iFLYTEK Inc. All Rights Reserved.
 */
class ISCAwakeActivity : IvwActivity() {

    companion object {
        const val TAG = "ISCAwakeActivity"
        const val WSS_URL = "wss://did.yawen.fun/ws/api/websocket/wake_android"

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    private val recorderCallback = object : RecorderCallback {

        override fun onStartRecord() {

        }

        override fun onPauseRecord() {
            menuEdDecibel?.title = ""
        }

        override fun onResumeRecord() {

        }

        override fun onRecordProgress(data: ByteArray, sampleSize: Int, volume: Int) {
            val calculateVolume = data.calculateVolume()
            menuEdDecibel?.title = "当前分贝:$calculateVolume"
        }

        override fun onStopRecord(output: File?) {
            menuEdDecibel?.title = ""
        }

    }

    override fun getRecorderCallback(): RecorderCallback {
        return recorderCallback
    }

    private fun sendStartCmd() {
        tvResult.append("sendStartCmd\n")
    }

    override fun onAbilityResult(key: String, value: String) {
        super.onAbilityResult(key, value)
        onVoiceAwake()
    }

    private fun onVoiceAwake() {
        lifecycleScope.launch {
                ivwHelper?.pauseAudioRecord()
                delay(500)
            //startVoiceAsr(true)

        }
    }

    override fun onAbilityError(code: Int, error: Throwable?) {
        super.onAbilityError(code, error)

    }

    override fun onAbilityEnd() {
        super.onAbilityEnd()
    }



    private fun onReceiveRestart() {
        appendMessage("websocket onReceiveRestart\n")
        lifecycleScope.launch {
            delay(500)
            ivwHelper?.resumeAudioRecord()
        }
    }


    private fun appendMessage(msg: String) {
        lifecycleScope.launch {
            tvResult.append(msg)
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}

data class Instruction(
    val type: Int,
) : Serializable {
    companion object {
        const val START = 1
        const val RESTART = 2
    }
}
package com.msspeech.demo.asr

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton

import com.qihoo.superbrain.voicewake2.media.audio.RecorderCallback
import com.qihoo.superbrain.voicewake2.tool.calculateVolume
import com.msspeech.demo.App
import com.msspeech.demo.R
import com.msspeech.demo.viewmodel.AsrViewModel
import com.msspeech.demo.viewmodel.AsrViewModelFactory
import com.qihoo.zhinao.demo.asr.IvwActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File


/**
 * @Desc: ivw唤醒
 * @Author leon
 * @Date 2023/2/23-17:14
 * Copyright 2023 iFLYTEK Inc. All Rights Reserved.
 */
class ISCAwakeActivity3 : IvwActivity() {

    companion object {
        const val TAG = "ISCAwakeActivity3"

        const val RESUME_LISTEN_TIMEOUT = 10 * 1000L

        val WAKE_KEYWORD =
            App.CONTEXT.getString(com.qihoo.superbrain.xunfeisdk.R.string.awake_keyword)
        const val THRESHOLD = 900
    }



    private lateinit var btnAudioRecord2: MaterialButton
    private lateinit var recordStatus: TextView
    private lateinit var websocketStatus: TextView
    private lateinit var commandStatus: TextView
    private lateinit var resumeAwake: TextView
    private lateinit var asrStatus: TextView
    private lateinit var debugInfos: View

    private var isAsrRunning = false
    private var isAsrInputting = false
    private var asrStartTime = 0L


    private fun initAsrObserve() {
        asrViewModel.let {
            it.asrInitResult.observe(this) { isSuccess ->
                Log.i(
                    TAG,
                    "init ASR isSuccess=$isSuccess"
                )
            }
            it.asrWaveData.observe(this) { waveData ->

            }
            it.asrStartEvent.observe(this) {

            }
            it.asrResultData.observe(this) { data ->
                if (data.event == AsrViewModel.EVENT_SENTENCE_START) {
                    isAsrInputting = true
                    asrStartTime = System.currentTimeMillis()
                    onVoiceAsrTextChange(data.asrText)
                    Log.e(TAG, "onNuiEventCallback start")
                } else if (data.event == AsrViewModel.EVENT_ASR_PARTIAL_RESULT) {
                    lifecycleScope.launch {
                        if (!isAsrRunning) {
                            return@launch
                        }
                        if (!isAsrInputting) {
                            return@launch
                        }
                        Log.i(
                            TAG,
                            "EVENT_ASR_PARTIAL_RESULT text=${data.asrText}"
                        )
                        onVoiceAsrTextChange(data.asrText)
                    }
                } else if (data.event == AsrViewModel.EVENT_SENTENCE_END) {
                    isAsrInputting = false
                    lifecycleScope.launch {
                        if (!isAsrRunning) {
                            return@launch
                        }
                        Log.i(TAG, "EVENT_SENTENCE_END text=${data.asrText}")
                        val result = replaceASRText(data.asrText)
                        onVoiceAsrTextChange(result)
                        onCompleteVoiceAsr(result)
                    }
                }
            }
        }
    }


    private fun replaceASRText(text: String): String {
        var result = text.replace("SC", "ISC", true)
        result = result.replace("IC", "ISC", true)
        result = result.replace("周红英", "周鸿祎")
        result = result.replace("周红衣", "周鸿祎")
        return result
    }

    private fun onCompleteVoiceAsr(asrText: String) {
        if (asrText.isNotBlank()) {
            stopVoiceAsr {
                lifecycleScope.launch {
                    delay(500)
                    resumeListenWake()
                }
            }
        } else {
            val deltaTime = System.currentTimeMillis() - asrStartTime
            if (deltaTime > 0 && deltaTime < 6 * 1000) {
                stopVoiceAsr {
                    lifecycleScope.launch {
                        delay(500)
                        startVoiceAsr(true)
                        postResumeListen(
                            clearBefore = true,
                            postNew = true,
                            timeout = 8 * 1000 - deltaTime
                        )
                    }
                }
            } else {
                postResumeListen(clearBefore = true, postNew = true, timeout = 0)
            }
        }
    }

    private fun sendTextMessage(asrText: String) {
        val json = JSONObject()
        json.put("type", 3)
        json.put("content", asrText)

        tvResult.append("sendTextMessage\n")
        updateStatus(commandStatus, "已发送文本")
        btnAudioRecord2.removeCallbacks(null)
        btnAudioRecord2.postDelayed({
            updateAsrText("", false)
        }, 2000)
    }

    private fun onVoiceAsrTextChange(resultText: String) {
        if (isAsrRunning) {
            postResumeListen(clearBefore = true, postNew = false)
            updateAsrText(resultText)
        }
    }


    private val resumeListenRunnable = Runnable {
        resetStatus()
    }


    suspend fun stopListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.stopAudioRecord()
    }

    suspend fun startListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.startAudioRecord(this@ISCAwakeActivity3, WAKE_KEYWORD, THRESHOLD)
    }

    suspend fun pauseListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.pauseAudioRecord()
        Log.e(TAG, "pauseListenWake")

    }

    suspend fun resumeListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.resumeAudioRecord()
        Log.e(TAG, "resumeListenWake")

    }


    private fun resetStatus() {
//        mViewModel.quitAudio()
        Log.i(TAG, "resetStatus text")
        updateAsrText("", false)
        asrStatus.text = "无"
        postResumeListen(clearBefore = true, postNew = false)
        stopVoiceAsr {
            lifecycleScope.launch {
                //showTalkView(false)
                delay(500)
                resumeListenWake()
            }
        }
    }

    fun startVoiceAsr(start: Boolean = false) {
        if (isAsrRunning) {
            return
        }
        isAsrRunning = true
        //showTalkView(true)
        updateAsrText("", true)
        Log.i(TAG, "startVoiceAsr start=$start")
        if (start) {
            asrViewModel.startDialog(::onStartDialogResult)
        }
    }

    fun updateAsrText(text: String?, isShow: Boolean = true) {
        asrText.text = text
        asrText.visibility = if (isShow) View.VISIBLE else View.GONE
    }


    fun onStartDialogResult(code: Int) {
        lifecycleScope.launch {
            Log.i(
                TAG,
                "onStartVoiceAsr code$code"
            )
            updateStatus(asrStatus, "已开启")
        }
    }


    fun stopVoiceAsr(callback: ((code: Long) -> Unit)? = null) {
        if (isAsrRunning) {
            asrViewModel.stopDialog { code ->
                Log.i(
                    TAG,
                    "stopVoiceAsr code$code"
                )
                callback?.invoke(code)
            }
            isAsrRunning = false
        } else {
            callback?.invoke(0)
        }
        updateStatus(asrStatus, "已关闭")
    }

    private fun postResumeListen(
        clearBefore: Boolean,
        postNew: Boolean,
        timeout: Long = RESUME_LISTEN_TIMEOUT
    ) {
        if (clearBefore) {
            btnAudioRecord2.removeCallbacks(resumeListenRunnable)
        }
        if (postNew) {
            btnAudioRecord2.postDelayed(resumeListenRunnable, timeout)
        }
    }

    private val asrViewModel by lazy {
        AsrViewModelFactory.getAsrViewModel(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initAsrObserve()
    }

    override fun getContentLayoutId(): Int {
        return R.layout.activity_ivw3
    }

    override fun onInitView() {
        debugInfos = findViewById<View?>(R.id.debug_infos)
        btnAudioRecord2 = findViewById<MaterialButton?>(R.id.btnAudioRecord2).apply {
            setOnLongClickListener {
                if (debugInfos.visibility == View.VISIBLE) {
                    debugInfos.visibility = View.GONE
                } else {
                    debugInfos.visibility = View.VISIBLE
                }
                false
            }
        }

        recordStatus = findViewById(R.id.record_status)
        websocketStatus = findViewById(R.id.websocket_status)
        commandStatus = findViewById(R.id.command_status)
        resumeAwake = findViewById<TextView?>(R.id.resume_awake).apply {
            setOnClickListener {
                onReceiveRestart()
            }
        }
        asrStatus = findViewById(R.id.asr_status)
        resumeAwake.visibility = View.GONE
        super.onInitView()
    }

    override fun getAudioRecordBtn(): MaterialButton {
        return btnAudioRecord2
    }


    private val recorderCallback = object : RecorderCallback {

        override fun onStartRecord() {
            updateStatus(recordStatus, "已启动录音(可唤醒)")
        }

        override fun onPauseRecord() {
            updateStatus(recordStatus, "已暂停录音（不可唤醒）")
            menuEdDecibel?.title = ""
        }

        override fun onResumeRecord() {
            updateStatus(recordStatus, "已恢复录音（可唤醒）")
        }

        override fun onRecordProgress(data: ByteArray, sampleSize: Int, volume: Int) {
            val calculateVolume = data.calculateVolume()
            menuEdDecibel?.title = "当前分贝:$calculateVolume"
        }

        override fun onStopRecord(output: File?) {
            updateStatus(recordStatus, "未开启（不可唤醒）")
            updateStatus(commandStatus, "无")
            updateStatus(asrStatus, "无")
            updateAsrText("", false)
            menuEdDecibel?.title = ""
        }

    }

    override fun getRecorderCallback(): RecorderCallback {
        return recorderCallback
    }

    override fun onAbilityResult(key: String, value: String) {
        super.onAbilityResult(key, value)
        Log.e("ISCAwake","onVoiceAwake")

        //   onVoiceAwake()
    }

    private fun onVoiceAwake() {
        Log.e("ISCAwake","onVoiceAwake")
        lifecycleScope.launch {
            pauseListenWake()
            delay(300)
            startVoiceAsr(true)
        }
    }

    override fun onAbilityError(code: Int, error: Throwable?) {
        super.onAbilityError(code, error)

    }

    override fun onAbilityEnd() {
        super.onAbilityEnd()
    }

    override fun onStart() {
        super.onStart()
        asrViewModel.initAsrSDK()
    }

    override fun onStop() {
        super.onStop()
        asrViewModel.releaseAsr()
    }



    private fun onReceiveRestart() {
        appendMessage("websocket onReceiveRestart\n")
        lifecycleScope.launch {
            updateStatus(commandStatus, "已收到恢复录音")
            delay(500)
            resumeListenWake()
        }
    }


    private fun appendMessage(msg: String) {
        lifecycleScope.launch {
            tvResult.append(msg)
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun updateStatus(textView: TextView, status: String) {
        lifecycleScope.launch {
            textView.text = status
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
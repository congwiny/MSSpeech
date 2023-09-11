package com.msspeech.demo.asr

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.msspeech.demo.viewmodel.AsrViewModel
import com.msspeech.demo.viewmodel.AsrViewModelFactory
import com.qihoo.zhinao.demo.asr.IvwActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MSAsrTestActivity: IvwActivity()  {

    private var isAsrRunning = false
    private var isAsrInputting = false
    private var asrStartTime = 0L

    
    private val asrViewModel by lazy {
        AsrViewModelFactory.getAsrViewModel(this)
    }

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initAsrObserve()
    }

    override fun onStart() {
        super.onStart()
        asrViewModel.initAsrSDK()
    }

    override fun onStop() {
        super.onStop()
        asrViewModel.releaseAsr()
    }


    override fun onAbilityResult(key: String, value: String) {
        super.onAbilityResult(key, value)
        onVoiceAwake()
    }

    private fun onVoiceAwake() {
        Log.e(TAG,"onVoiceAwake")
        lifecycleScope.launch {
            pauseListenWake()
            delay(300)
            startVoiceAsr(true)
        }
    }

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
                        onVoiceAsrTextChange(data.asrText)
                        onCompleteVoiceAsr(data.asrText)
                    }
                }
            }
        }
    }

    private val resumeListenRunnable = Runnable {
        resetStatus()
    }


    suspend fun stopListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.stopAudioRecord()
    }

    suspend fun startListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.startAudioRecord(this@MSAsrTestActivity, ISCAwakeActivity3.WAKE_KEYWORD, ISCAwakeActivity3.THRESHOLD)
    }

    suspend fun pauseListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.pauseAudioRecord()
        Log.e(TAG, "pauseListenWake")

    }

    suspend fun resumeListenWake() = withContext(Dispatchers.IO) {
        ivwHelper?.resumeAudioRecord()
        Log.e(TAG, "resumeListenWake")

    }

    private fun onVoiceAsrTextChange(resultText: String) {
        if (isAsrRunning) {
            postResumeListen(clearBefore = true, postNew = false)
            updateAsrText(resultText)
        }
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
    
    private fun resetStatus() {
//        mViewModel.quitAudio()
        Log.i(TAG, "resetStatus text")
        updateAsrText("", false)
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
    }


    private fun updateStatus(textView: TextView, status: String) {
//        lifecycleScope.launch {
//            textView.text = status
//        }
    }

    fun onStartDialogResult(code: Int) {
        lifecycleScope.launch {
            Log.i(
                TAG,
                "onStartVoiceAsr code$code"
            )
            updateStatus(asrText, "已开启")
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
        updateStatus(asrText, "已关闭")
    }

    private fun postResumeListen(
        clearBefore: Boolean,
        postNew: Boolean,
        timeout: Long = ISCAwakeActivity3.RESUME_LISTEN_TIMEOUT
    ) {
        if (clearBefore) {
            getAudioRecordBtn().removeCallbacks(resumeListenRunnable)
        }
        if (postNew) {
            getAudioRecordBtn().postDelayed(resumeListenRunnable, timeout)
        }
    }


    companion object{
        const val TAG = "MSAsrTestActivity"
    }
}
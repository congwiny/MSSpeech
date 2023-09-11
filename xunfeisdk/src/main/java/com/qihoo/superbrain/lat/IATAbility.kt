package com.qihoo.superbrain.lat

import android.content.Context
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.RecognizerListener
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechRecognizer

class IATAbility {

    private var mIat: SpeechRecognizer? = null

    fun init(context: Context, listener: InitListener) {
        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(context, listener)
        setParams(context)
    }

    fun setParams(context: Context) {
        mIat?.run {
            // 清空参数
            setParameter(SpeechConstant.PARAMS, null)
            // 设置听写引擎
            setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
            // 设置返回结果格式
            setParameter(SpeechConstant.RESULT_TYPE, "json")
            setParameter(SpeechConstant.LANGUAGE, "zh_cn")
            // 设置语言区域
            setParameter(SpeechConstant.ACCENT, "mandarin")
            // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理

            //此处用于设置dialog中不显示错误码信息
            //mIat.setParameter("view_tips_plain","false");
            // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
            setParameter(SpeechConstant.VAD_BOS, "8000")
            // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
            setParameter(
                SpeechConstant.VAD_EOS, "1500"
            )
            setParameter("dwa", "wpgs")
            // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
            setParameter(SpeechConstant.ASR_PTT, "1")
            // 设置音频保存路径，保存音频格式支持pcm、wav.
            setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
            setParameter(
                SpeechConstant.ASR_AUDIO_PATH,
                context.getExternalFilesDir("msc")?.absolutePath + "/iat.wav"
            )
        }

    }

    fun startListen(listener: RecognizerListener): Int {
        // 不显示听写对话框
        return mIat?.startListening(listener) ?: CODE_START_LISTEN_FAILED
    }

    fun stopListen() {
        mIat?.stopListening()
    }

    fun isListening(): Boolean {
        return mIat?.isListening ?: false
    }

    fun cancel() {
        mIat?.cancel()
    }

    fun destroy() {
        mIat?.run {
            cancel()
            destroy()
        }
    }

    companion object{
        const val CODE_SUCCESS = 0
        const val CODE_SDK_NOT_INIT = -1
        const val CODE_START_LISTEN_FAILED = -2
        const val CODE_CANNOT_STOP_LISTEN = -3

    }
}
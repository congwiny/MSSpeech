package com.msspeech.demo.viewmodel

import android.os.Bundle
import android.util.Log

import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.RecognizerListener
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.SpeechError
import com.qihoo.superbrain.lat.IATAbility
import com.qihoo.superbrain.lat.IATAbility.Companion.CODE_CANNOT_STOP_LISTEN
import com.qihoo.superbrain.lat.IATAbility.Companion.CODE_SDK_NOT_INIT
import com.qihoo.superbrain.lat.IATAbility.Companion.CODE_SUCCESS
import com.qihoo.superbrain.lat.utils.JsonParser
import com.msspeech.demo.App
import org.json.JSONException
import org.json.JSONObject

class XFAsrViewModel : AsrViewModel() {


    private val mIatResults: HashMap<String, String> = LinkedHashMap()

    val iatAbility by lazy { IATAbility() }

    override fun initAsrSDK() {
        if (!isSDKInited && !isInitingSDK) {
            isInitingSDK = true
            iatAbility.init(App.CONTEXT) { code ->
                val result = code == ErrorCode.SUCCESS
                asrInitResult.postValue(result)
                isInitingSDK = false
                isSDKInited = result
                Log.i(TAG, "initAsrSDK result=$result")
            }
        }
    }

    override fun releaseAsr() {
        if (!isSDKInited) {
            return
        }
        isSDKInited = false
        iatAbility.destroy()
    }

    private val recognizerListener by lazy {

        object : RecognizerListener {

            override fun onBeginOfSpeech() {
                // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
                asrStartEvent.postValue(true)
                asrResultData.postValue(
                    AsrResultData(
                        EVENT_SENTENCE_START,
                        ""
                    )
                )
            }

            override fun onError(error: SpeechError) {
                // Tips：
                // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
                Log.d(TAG, "onError " + error.getPlainDescription(true))
                //iatAbility.cancel()
            }

            override fun onEndOfSpeech() {
                // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            }

            override fun onResult(results: RecognizerResult, isLast: Boolean) {
                parseRecognizerResult(results, isLast)
            }

            override fun onVolumeChanged(volume: Int, data: ByteArray) {
                //showTip("当前正在说话，音量大小 = " + volume + " 返回音频数据 = " + data.size)
                asrWaveData.postValue(data)
            }

            override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle?) {
                // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
                // 若使用本地能力，会话id为null
                //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                //		Log.d(TAG, "session id =" + sid);
                //	}
            }


            private fun parseRecognizerResult(results: RecognizerResult, isLast: Boolean) {
                Log.d(TAG, results.resultString)
                if (isLast) {
                    Log.d(TAG, "onResult 结束")
                }
                val text: String = JsonParser.parseIatResult(results.resultString)
                Log.d(TAG, "parseRecognizerResult text=$text")
                var sn: String? = null
                var pgs: String? = null
                var rg: String? = null
                // 读取json结果中的sn字段
                try {
                    val resultJson = JSONObject(results.resultString)
                    sn = resultJson.optString("sn")
                    pgs = resultJson.optString("pgs")
                    rg = resultJson.optString("rg")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                //如果pgs是rpl就在已有的结果中删除掉要覆盖的sn部分
                if (pgs == "rpl") {
                    val strings = rg!!.replace("[", "").replace("]", "").split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val begin = strings[0].toInt()
                    val end = strings[1].toInt()
                    for (i in begin..end) {
                        mIatResults.remove(i.toString() + "")
                    }
                }
                mIatResults[sn!!] = text
                val resultBuffer = StringBuffer()
                for (key in mIatResults.keys) {
                    resultBuffer.append(mIatResults[key])
                }
                val event =
                    if (isLast) EVENT_SENTENCE_END else EVENT_ASR_PARTIAL_RESULT
                asrResultData.postValue(
                    AsrResultData(
                        event,
                        resultBuffer.toString()
                    )
                )
            }
        }
    }

    override fun startDialog(callback: ((code: Int) -> Unit)?) {
        if (!isSDKInited) {
            callback?.invoke(CODE_SDK_NOT_INIT)
            return
        }
        mIatResults.clear()
        iatAbility.setParams(App.CONTEXT)
        val result = iatAbility.startListen(recognizerListener)
        callback?.invoke(result)
    }

    override fun stopDialog(callback: ((code: Long) -> Unit)?) {
        if (!isSDKInited) {
            callback?.invoke(CODE_SDK_NOT_INIT.toLong())
            return
        }
        if (!iatAbility.isListening()) {
            callback?.invoke(CODE_CANNOT_STOP_LISTEN.toLong())
            return
        }
        mIatResults.clear()
        iatAbility.stopListen()
        callback?.invoke(CODE_SUCCESS.toLong())
    }


}
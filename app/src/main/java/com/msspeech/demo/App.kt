package com.msspeech.demo

import android.app.Application
import android.content.Context
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility
import com.qihoo.superbrain.AppContextWrapper
import kotlin.properties.Delegates

class App : Application() {

    companion object {
        var CONTEXT: Context by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = applicationContext
        AppContextWrapper.appContext = applicationContext
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=${getString(com.qihoo.superbrain.xunfeisdk.R.string.appId)}")
    }
}

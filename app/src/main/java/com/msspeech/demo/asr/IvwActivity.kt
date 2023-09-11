package com.qihoo.zhinao.demo.asr

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.qihoo.superbrain.voicewake2.ability.AbilityCallback
import com.qihoo.superbrain.voicewake2.ability.AbilityConstant
import com.qihoo.superbrain.voicewake2.ability.abilityAuthStatus
import com.qihoo.superbrain.voicewake2.ability.ivw.IvwHelper
import com.qihoo.superbrain.voicewake2.media.audio.RecorderCallback
import com.qihoo.superbrain.voicewake2.tool.calculateVolume
import com.qihoo.superbrain.voicewake2.tool.setChildrenEnabled
import com.msspeech.demo.App
import com.msspeech.demo.R
import com.msspeech.demo.asr.BaseActivity
import com.msspeech.demo.widget.CustomSeekBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


/**
 * @Desc: ivw唤醒
 * @Author leon
 * @Date 2023/2/23-17:14
 * Copyright 2023 iFLYTEK Inc. All Rights Reserved.
 */
open class IvwActivity : BaseActivity(), AbilityCallback {

    private val TAG = "IvwActivity"

    protected lateinit var tvKeyword: AppCompatEditText
    protected lateinit var audioGroup: RadioGroup
    protected lateinit var radioAudioRecord: AppCompatRadioButton
    protected lateinit var radioAudioFile: AppCompatRadioButton
    protected lateinit var btnAudioRecord: MaterialButton
    protected lateinit var tvAudioRecord: AppCompatTextView
    protected lateinit var btnAudioFile: MaterialButton
    protected lateinit var tvResult: AppCompatTextView
    protected lateinit var progressThreshold: CustomSeekBar
    protected var menuEdDecibel: MenuItem? = null
    protected lateinit var asrText: TextView

    protected lateinit var scrollView: NestedScrollView


    protected var ivwHelper: IvwHelper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentLayoutId())
        onInitView()
        audioRecordListener(getListenText(), false)
    }

    protected open fun onInitView() {
        tvKeyword = findViewById(R.id.tvKeyword)
        audioGroup = findViewById(R.id.audioGroup)
        radioAudioRecord = findViewById(R.id.radioAudioRecord)
        radioAudioFile = findViewById(R.id.radioAudioFile)
        btnAudioRecord = findViewById(R.id.btnAudioRecord)
        tvAudioRecord = findViewById(R.id.tvAudioRecord)
        btnAudioFile = findViewById(R.id.btnAudioFile)
        tvResult = findViewById(R.id.tvResult)
        scrollView = findViewById(R.id.scroll_view)
        asrText = findViewById(R.id.asrText)
        progressThreshold = findViewById(R.id.progressThreshold)
        progressThreshold.apply {
            setMaxProgress(3000)
            bindData("门限值", 900) {}
        }
        ivwHelper = IvwHelper(this).apply {
            setRecorderCallback(getRecorderCallback())
        }
        tvResult.append(AbilityConstant.IVW_ID.abilityAuthStatus())

        audioGroup.setOnCheckedChangeListener { _, i ->
            audioButtonVisible(i == R.id.radioAudioRecord)
        }

        audioGroup.check(R.id.radioAudioRecord)

        btnAudioFile.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val fs = App.CONTEXT.assets.open("ivw_xiaoduxiaodu.pcm")
                val filePath = IvwHelper.createKeywordFile(
                    applicationContext,
                    tvKeyword.text.toString().trim()
                )
                kotlin.runCatching {
                    val keywordSize = tvKeyword.text.toString().trim().split(";").count()
                    ivwHelper?.writeStream(
                        fs,
                        filePath,
                        keywordSize,
                        progressThreshold.getProgress()
                    )
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }


    protected open fun getContentLayoutId(): Int {
        return R.layout.activity_ivw
    }

    protected open fun getListenText(isListen: Boolean = true): String {
        return if (isListen) getString(R.string.start_listen) else getString(R.string.stop_listen)
    }

    protected open fun getAudioRecordBtn(): MaterialButton {
        return btnAudioRecord
    }

    protected open fun audioRecordListener(text: String, check: Boolean) {
        getAudioRecordBtn().apply {
            clearOnCheckedChangeListeners()
            this.text = text
            this.isChecked = check
            this.isSelected = check
            addOnCheckedChangeListener { button, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if (isChecked) {
                        ivwHelper?.startAudioRecord(
                            applicationContext,
                            tvKeyword.text.toString(),
                            progressThreshold.getProgress()
                        )
                    } else {
                        ivwHelper?.stopAudioRecord()
                    }
                }
                button.isSelected = isChecked
                this.text = if (isChecked) getListenText(false) else getListenText()
            }
        }
    }


    protected open fun audioButtonVisible(audioVisible: Boolean) {
        getAudioRecordBtn().isVisible = audioVisible
//        tvAudioRecord.isVisible = audioVisible
        btnAudioFile.isVisible = !audioVisible
    }

    protected open fun audioButtonEnable(enable: Boolean) {
        audioGroup.setChildrenEnabled(enable)
        tvKeyword.isEnabled = enable
    }

    override fun onAbilityBegin() {
        tvResult.append("语音唤醒开始\n")
        audioButtonEnable(false)
    }

    override fun onAbilityResult(key: String, value: String) {
        tvResult.append("${key}: ${value}\n")
    }

    override fun onAbilityError(code: Int, error: Throwable?) {
        audioButtonEnable(true)
        audioRecordListener(getListenText(), false)
        tvResult.append("语音唤醒error---$code, msg=${error?.message}")
    }

    override fun onAbilityEnd() {
        tvResult.append("语音唤醒结束---")
        audioButtonEnable(true)
        audioRecordListener(getListenText(), false)
    }

    private val recorderCallback = object : RecorderCallback {

        override fun onStartRecord() {}

        override fun onPauseRecord() {
        }

        override fun onResumeRecord() {
        }

        override fun onRecordProgress(data: ByteArray, sampleSize: Int, volume: Int) {
            val calculateVolume = data.calculateVolume()
            menuEdDecibel?.title = "当前分贝:$calculateVolume"
        }

        override fun onStopRecord(output: File?) {
        }

    }

    protected open fun getRecorderCallback(): RecorderCallback {
        return recorderCallback
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_esr_ed, menu)
        menu.findItem(R.id.ed_setting).isVisible = false
        menuEdDecibel = menu.findItem(R.id.ed_decibel)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        ivwHelper?.destroy()
    }
}
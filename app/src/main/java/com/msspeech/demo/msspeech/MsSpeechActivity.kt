package com.msspeech.demo.msspeech

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.msspeech.demo.databinding.ActivityMsspeechBinding
import com.permissionx.guolindev.PermissionX


class MsSpeechActivity : FragmentActivity() {

    private val mViewModel: MsSpeechViewModel by lazy {
        ViewModelProvider(this).get(MsSpeechViewModel::class.java).apply {
            speechResult.observe(this@MsSpeechActivity) {
                mBinding.speechResultTv.text = it
            }
            isRecognizing.observe(this@MsSpeechActivity) { isStarted ->
                mBinding.speechBtn.apply {
                    text = if (isStarted) "识别中" else "开始识别"
                    isEnabled = !isStarted
                }
            }
            awakeKeyword.observe(this@MsSpeechActivity) {
                mBinding.speechResultTv.text = it
                if (!it.isNullOrBlank()) {
                    startSpeechOnce()
                }
            }
        }
    }

    private val mBinding: ActivityMsspeechBinding by lazy {
        ActivityMsspeechBinding.inflate(layoutInflater).apply {
            speechBtn.setOnClickListener {
                mViewModel.startKeywordRecognize()
                //mViewModel.startSpeechOnce()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        PermissionX.init(this).permissions(android.Manifest.permission.RECORD_AUDIO)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    doOnCreate()
                } else {
                    Toast.makeText(this, "请授予录音权限", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun doOnCreate() {
        mViewModel.initialize()
    }
}
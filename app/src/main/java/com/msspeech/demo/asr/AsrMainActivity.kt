package com.msspeech.demo.asr

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.msspeech.demo.R
import com.qihoo.superbrain.voicewake2.ability.IFlytekAbilityManager
import com.qihoo.superbrain.voicewake2.tool.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AsrMainActivity : BaseActivity() {

    private var btnIvw: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBarNavigation(false)
        setContentView(R.layout.asr_activity_main)
        btnIvw = findViewById(R.id.btnIvw)
        btnIvw?.isEnabled = false
        activityResultLauncher.launch(
            arrayListOf(Manifest.permission.RECORD_AUDIO).apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                    add(Manifest.permission.READ_MEDIA_VIDEO)
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                }
            }.toTypedArray()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
        findViewById<AppCompatButton>(R.id.btnIvw).setOnClickListener {
            startActivity(Intent(this, MSAsrTestActivity::class.java))
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        IFlytekAbilityManager.getInstance().initializeSdk(applicationContext)
                    }
                    btnIvw?.isEnabled = true
                }
            }
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    // Permission is granted
                } else {
                    // Permission is denied
                    toast("${permissionName}被拒绝了，请在应用设置里打开权限")
                }
            }
        }
}
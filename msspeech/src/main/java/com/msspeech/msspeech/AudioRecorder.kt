package com.msspeech.msspeech

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.msspeech.msspeech.utils.mainThread
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder : Recorder {

    private val TAG = "AudioRecorder"

    var audioRecord: AudioRecord? = null

    private var recordCallback: RecorderCallback? = null

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)


    @SuppressLint("MissingPermission")
    fun init() {
        if (null != audioRecord) {
            audioRecord?.release()
        }
        try {
            // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
            val af = AudioFormat.Builder()
                .setSampleRate(MicrophoneStream.SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()
            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(af)
                .build()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw AudioRecordException("初始化录音失败")
        }
    }

    override fun setRecorderCallback(callback: RecorderCallback?) {
        recordCallback = callback
    }


    fun read(audioData: ByteArray, offsetInBytes:Int, sizeInBytes:Int):Int {
       return this.audioRecord?.read(audioData, offsetInBytes, sizeInBytes)?:-1
    }
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording() {
        when (audioRecord?.state) {
            AudioRecord.STATE_INITIALIZED -> {
                try {
                    audioRecord?.startRecording()
                } catch (e: Exception) {
                    throw AudioRecordException("录音失败")
                }
            }
            AudioRecord.STATE_UNINITIALIZED -> {
                init()
                audioRecord?.startRecording()
            }
            else -> {
                throw AudioRecordException("录音失败")
            }
        }
        isRecording.set(true)
        isPaused.set(false)

        try {
            mainThread {
                recordCallback?.onStartRecord()
            }
        } catch (e: Exception) {
            throw AudioRecordException("录音失败")
        }
    }


    override fun resumeRecording() {
        if (audioRecord != null && audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
            if (isPaused.get()) {
                audioRecord?.startRecording()
                mainThread {
                    recordCallback?.onResumeRecord()
                }
                isPaused.set(false)
            }
        }
    }

    override fun pauseRecording() {
        if (audioRecord != null && isRecording.get()) {
            audioRecord?.stop()
            isPaused.set(true)
            mainThread {
                recordCallback?.onPauseRecord()
            }
        }
    }

    override fun stopRecording() {
        if (audioRecord != null) {
            isRecording.set(false)
            isPaused.set(false)
            if (audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    audioRecord?.stop()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "stopRecording() problems", e)
                }
            }
            audioRecord?.release()
        }
    }

    override fun isRecording(): Boolean {
        return isRecording.get()
    }

    override fun isPaused(): Boolean {
        return isPaused.get()
    }


}
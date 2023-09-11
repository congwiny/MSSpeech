//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//
package com.msspeech.msspeech;

import android.annotation.SuppressLint;

import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;

/**
 * MicrophoneStream exposes the Android Microphone as an PullAudioInputStreamCallback
 * to be consumed by the Speech SDK.
 * It configures the microphone with 16 kHz sample rate, 16 bit samples, mono (single-channel).
 */
public class MicrophoneStream extends PullAudioInputStreamCallback {
    public final static int SAMPLE_RATE = 16000;
    private final AudioStreamFormat format;
    private AudioRecorder recorder;


    public MicrophoneStream() {
        this.format = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE, (short)16, (short)1);
        this.initMic();
    }

    public AudioStreamFormat getFormat() {
        return this.format;
    }

    @Override
    public int read(byte[] bytes) {
        if (this.recorder != null) {
            long ret = this.recorder.read(bytes, 0, bytes.length);
            return (int) ret;
        }
        return 0;
    }

    @Override
    public void close() {
        if (this.recorder!=null){
            this.recorder.stopRecording();
        }
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (this.recorder!=null){
            this.recorder.startRecording();
        }
    }

    @SuppressLint("MissingPermission")
    public void resume(){
        if (this.recorder!=null){
            this.recorder.resumeRecording();
        }
    }
    public void pause(){
        if (this.recorder!=null){
            this.recorder.pauseRecording();
        }
    }

    @SuppressLint("MissingPermission")
    private void initMic() {
        // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
        this.recorder = new AudioRecorder();
        this.recorder.init();

    }
}
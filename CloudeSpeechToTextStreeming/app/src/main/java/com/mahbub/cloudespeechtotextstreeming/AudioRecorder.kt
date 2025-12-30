package com.mahbub.cloudespeechtotextstreeming

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

interface AudioRecorder {
    suspend fun startRecording(onAudioChunk: (ByteArray) -> Unit)
    fun stopRecording()
}

class AndroidAudioRecorder : AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    @SuppressLint("MissingPermission")
    override suspend fun startRecording(onAudioChunk: (ByteArray) -> Unit) = withContext(Dispatchers.IO) @androidx.annotation.RequiresPermission(
        android.Manifest.permission.RECORD_AUDIO
    ) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("Unable to get minimum buffer size")
        }

        val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }

        audioRecord?.startRecording()
        isRecording = true

        val chunkSize = 4096
        val buffer = ByteArray(chunkSize * 2)

        while (isRecording && coroutineContext.isActive) {
            val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0

            if (readBytes > 0) {
                val chunk = buffer.copyOf(readBytes)
                onAudioChunk(chunk)
            } else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                break
            } else if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
                break
            }
        }
    }

    override fun stopRecording() {
        isRecording = false
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
    }
}


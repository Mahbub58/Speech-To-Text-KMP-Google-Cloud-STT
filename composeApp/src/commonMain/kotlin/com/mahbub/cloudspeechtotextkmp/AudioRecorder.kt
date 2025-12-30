package com.mahbub.cloudspeechtotextkmp

interface AudioRecorder {
    suspend fun startRecording(onAudioChunk: (ByteArray) -> Unit)
    fun stopRecording()
}

expect fun createAudioRecorder(): AudioRecorder


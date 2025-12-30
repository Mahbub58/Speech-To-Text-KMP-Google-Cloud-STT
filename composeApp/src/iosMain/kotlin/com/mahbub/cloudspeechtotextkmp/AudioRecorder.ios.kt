package com.mahbub.cloudspeechtotextkmp

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioInputNode
import platform.AVFAudio.AVAudioPCMBuffer

class IOSAudioRecorder : AudioRecorder {

    private var audioEngine: AVAudioEngine? = null
    private var inputNode: AVAudioInputNode? = null

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun startRecording(onAudioChunk: (ByteArray) -> Unit) {
        val engine = AVAudioEngine()
        audioEngine = engine

        val input = engine.inputNode
        inputNode = input

        val format = input.outputFormatForBus(0u)

        input.installTapOnBus(
            bus = 0u,
            bufferSize = 4096u,
            format = format
        ) { buffer, _ ->
            val pcmBuffer = buffer as? AVAudioPCMBuffer ?: return@installTapOnBus
            val frameLength = pcmBuffer.frameLength.toInt()
            val channelDataPtr = pcmBuffer.floatChannelData ?: return@installTapOnBus
            val channelData = channelDataPtr[0] ?: return@installTapOnBus

            val sourceSampleRate = format.sampleRate
            val targetSampleRate = 16000.0

            val ratio = sourceSampleRate / targetSampleRate
            val outputSamples = if (ratio > 1.0) {
                (frameLength / ratio).toInt()
            } else {
                frameLength
            }

            val bytes = outputSamples * 2
            val result = ByteArray(bytes)

            result.usePinned { pinned ->
                val dst: CPointer<ByteVar> = pinned.addressOf(0)
                var i = 0
                var srcPos = 0.0
                while (i < outputSamples && srcPos < frameLength) {
                    val srcIndex = srcPos.toInt()
                    val floatSample = channelData[srcIndex]
                    var intSample = (floatSample * 32767.0f).toInt()
                    if (intSample > 32767) intSample = 32767
                    if (intSample < -32768) intSample = -32768

                    val idx = i * 2
                    dst[idx] = (intSample and 0xFF).toByte()
                    dst[idx + 1] = ((intSample shr 8) and 0xFF).toByte()

                    srcPos += if (ratio > 1.0) ratio else 1.0
                    i++
                }
            }

            onAudioChunk(result)
        }

        engine.prepare()
        engine.startAndReturnError(null)
    }

    override fun stopRecording() {
        inputNode?.removeTapOnBus(0u)
        audioEngine?.stop()
        audioEngine = null
        inputNode = null
    }
}

actual fun createAudioRecorder(): AudioRecorder = IOSAudioRecorder()

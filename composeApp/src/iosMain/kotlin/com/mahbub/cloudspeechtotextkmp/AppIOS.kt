package com.mahbub.cloudspeechtotextkmp

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

private var iosTtsPlayer: AVAudioPlayer? = null

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }

@OptIn(ExperimentalForeignApi::class)
private fun playTtsAudioIOS(audio: ByteArray) {
    val data = audio.toNSData()
    iosTtsPlayer?.stop()
    iosTtsPlayer = AVAudioPlayer(data, null).apply {
        prepareToPlay()
        play()
    }
}

@Composable
actual fun SpeechAppContent() {
    val controller = rememberSpeechRecognitionController()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
//        SpeechScreen(
//            controller = controller,
//            modifier = Modifier
//        )

        Spacer(modifier = Modifier.height(32.dp))

        TtsScreen(
            modifier = Modifier,
            onPlayAudio = { audio ->
                playTtsAudioIOS(audio)
            }
        )
    }
}

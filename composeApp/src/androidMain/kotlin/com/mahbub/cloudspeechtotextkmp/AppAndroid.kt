package com.mahbub.cloudspeechtotextkmp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File

private var ttsMediaPlayer: MediaPlayer? = null

private fun playTtsAudio(context: android.content.Context, audio: ByteArray) {
    val tempFile = File.createTempFile("tts_", ".mp3", context.cacheDir)
    tempFile.outputStream().use { it.write(audio) }

    ttsMediaPlayer?.release()
    val player = MediaPlayer()
    player.setDataSource(tempFile.absolutePath)
    player.setOnCompletionListener {
        tempFile.delete()
        it.release()
        if (ttsMediaPlayer === it) {
            ttsMediaPlayer = null
        }
    }
    player.prepare()
    player.start()
    ttsMediaPlayer = player
}

@Composable
actual fun SpeechAppContent() {
    val controller = rememberSpeechRecognitionController()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            controller.startRecording()
        }
    }

    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val isRecordingState = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
//        SpeechScreen(
//            controller = controller,
//            modifier = Modifier,
//            onToggleRecording = {
//                if (isRecordingState.value) {
//                    controller.stopRecording()
//                    isRecordingState.value = false
//                } else {
//                    if (hasRecordPermission) {
//                        controller.startRecording()
//                        isRecordingState.value = true
//                    } else {
//                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//                    }
//                }
//            },
//            isRecordingOverride = isRecordingState.value
//        )

        Spacer(modifier = Modifier.height(32.dp))

        TtsScreen(
            modifier = Modifier,
            onPlayAudio = { audio ->
                playTtsAudio(context, audio)
            }
        )
    }
}

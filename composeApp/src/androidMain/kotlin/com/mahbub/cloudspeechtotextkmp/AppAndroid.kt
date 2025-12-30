package com.mahbub.cloudspeechtotextkmp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

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

    SpeechScreen(
        controller = controller,
        modifier = Modifier,
        onToggleRecording = {
            if (isRecordingState.value) {
                controller.stopRecording()
                isRecordingState.value = false
            } else {
                if (hasRecordPermission) {
                    controller.startRecording()
                    isRecordingState.value = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        },
        isRecordingOverride = isRecordingState.value
    )
}


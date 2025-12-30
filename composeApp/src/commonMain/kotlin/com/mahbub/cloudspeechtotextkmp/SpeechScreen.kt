package com.mahbub.cloudspeechtotextkmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun rememberSpeechRecognitionController(): SpeechRecognitionController {
    val controller = remember {
        val httpClient = createSpeechHttpClient()
        val dataSource = WebSocketSpeechDataSource(httpClient)
        val audioRecorder = createAudioRecorder()
        SpeechRecognitionController(
            webSocketDataSource = dataSource,
            audioRecorder = audioRecorder
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.clear()
        }
    }

    return controller
}

@Composable
fun SpeechScreen(
    controller: SpeechRecognitionController,
    modifier: Modifier = Modifier,
    onToggleRecording: (() -> Unit)? = null,
    isRecordingOverride: Boolean? = null
) {
    val state by controller.transcriptionState.collectAsState()
    val isRecordingState = remember { mutableStateOf(state is SpeechRecognitionController.TranscriptionState.Recording) }
    val isRecording = isRecordingOverride ?: isRecordingState.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (state) {
                is SpeechRecognitionController.TranscriptionState.Recording -> "Listening..."
                is SpeechRecognitionController.TranscriptionState.Completed -> "Session completed"
                is SpeechRecognitionController.TranscriptionState.Error -> "Error"
                SpeechRecognitionController.TranscriptionState.Idle -> "Idle"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRecording) {
            val recordingState = state as? SpeechRecognitionController.TranscriptionState.Recording
            if (recordingState != null) {
                Text(
                    text = "Partial: ${recordingState.partialText}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Final: ${recordingState.finalText}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (state is SpeechRecognitionController.TranscriptionState.Completed) {
            val completedState = state as SpeechRecognitionController.TranscriptionState.Completed
            Text(
                text = completedState.text,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (state is SpeechRecognitionController.TranscriptionState.Error) {
            val errorState = state as SpeechRecognitionController.TranscriptionState.Error
            Text(
                text = errorState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (onToggleRecording != null) {
                    onToggleRecording()
                } else {
                    if (isRecordingState.value) {
                        controller.stopRecording()
                        isRecordingState.value = false
                    } else {
                        controller.startRecording()
                        isRecordingState.value = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRecording) {
                    "Stop recording"
                } else {
                    "Start recording"
                }
            )
        }
    }
}

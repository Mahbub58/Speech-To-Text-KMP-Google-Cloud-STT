package com.mahbub.cloudespeechtotextstreeming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mahbub.cloudespeechtotextstreeming.ui.theme.CloudeSpeechToTextStreemingTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

class MainActivity : ComponentActivity() {

    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(WebSockets)
        }
    }

    private val webSocketDataSource by lazy {
        WebSocketSpeechDataSource(httpClient)
    }

    private val audioRecorder by lazy {
        AndroidAudioRecorder()
    }

    private val speechViewModel: SpeechRecognitionViewModel by viewModels {
        SpeechRecognitionViewModelFactory(
            webSocketDataSource = webSocketDataSource,
            audioRecorder = audioRecorder
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CloudeSpeechToTextStreemingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpeechScreen(
                        viewModel = speechViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeechScreen(
    viewModel: SpeechRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.transcriptionState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        }
    }

    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val isRecording = remember { mutableStateOf(state is SpeechRecognitionViewModel.TranscriptionState.Recording) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (state) {
                is SpeechRecognitionViewModel.TranscriptionState.Recording -> "Listening..."
                is SpeechRecognitionViewModel.TranscriptionState.Completed -> "Session completed"
                is SpeechRecognitionViewModel.TranscriptionState.Error -> "Error"
                SpeechRecognitionViewModel.TranscriptionState.Idle -> "Idle"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRecording.value) {
            val recordingState = state as SpeechRecognitionViewModel.TranscriptionState.Recording
            Text(
                text = "Partial: ${recordingState.partialText}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Final: ${recordingState.finalText}",
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (state is SpeechRecognitionViewModel.TranscriptionState.Completed) {
            val completedState = state as SpeechRecognitionViewModel.TranscriptionState.Completed
            Text(
                text = completedState.text,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (state is SpeechRecognitionViewModel.TranscriptionState.Error) {
            val errorState = state as SpeechRecognitionViewModel.TranscriptionState.Error
            Text(
                text = errorState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isRecording.value) {
                    viewModel.stopRecording()
                    isRecording.value = false
                } else {
                    if (hasRecordPermission) {
                        viewModel.startRecording()
                        isRecording.value = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRecording.value) {
                    "Stop recording"
                } else {
                    "Start recording"
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpeechScreenPreview() {
    CloudeSpeechToTextStreemingTheme {
    }
}

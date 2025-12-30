package com.mahbub.cloudspeechtotextkmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun rememberGoogleTextToSpeechClient(): GoogleTextToSpeechClient {
    val client = remember {
        GoogleTextToSpeechClient(createSpeechHttpClient())
    }
    return client
}

@Composable
fun TtsScreen(
    modifier: Modifier = Modifier,
    onPlayAudio: (ByteArray) -> Unit
) {
    val ttsClient = rememberGoogleTextToSpeechClient()
    var text by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Text to Speech",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter text") }
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            println("TTS request failed: HTTP ${errorMessage?: "Unknown error"}")
        }


        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    if (text.isBlank() || isLoading) {
                        return@launch
                    }
                    isLoading = true
                    errorMessage = null
                    try {
                        val audio = ttsClient.synthesize(
                            text = text,
                            languageCode = "en-US"
                        )
                        onPlayAudio(audio)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "TTS error"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isLoading) "Loading..." else "Play")
        }
    }
}


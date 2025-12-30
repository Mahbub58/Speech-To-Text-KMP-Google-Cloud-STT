package com.mahbub.cloudspeechtotextkmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SpeechAppContent() {
    val controller = rememberSpeechRecognitionController()
    SpeechScreen(
        controller = controller,
        modifier = Modifier
    )
}


package com.mahbub.cloudspeechtotextkmp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SpeechRecognitionController(
    private val webSocketDataSource: WebSocketSpeechDataSource,
    private val audioRecorder: AudioRecorder,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState

    private var recognitionJob: Job? = null
    private var recordingJob: Job? = null

    fun startRecording(languageCode: String = "bn-IN") {
        if (_transcriptionState.value is TranscriptionState.Recording) {
            return
        }

        recognitionJob = scope.launch {
            try {
                _transcriptionState.value = TranscriptionState.Recording(
                    partialText = "",
                    finalText = ""
                )

                webSocketDataSource.startStreamingRecognition(languageCode)
                    .catch { e ->
                        _transcriptionState.value = TranscriptionState.Error(e.message ?: "Unknown error")
                    }
                    .collect { result ->
                        when (result) {
                            is WebSocketSpeechDataSource.TranscriptionResult.Partial -> {
                                val currentState = _transcriptionState.value
                                if (currentState is TranscriptionState.Recording) {
                                    _transcriptionState.value = currentState.copy(
                                        partialText = result.text
                                    )
                                }
                            }

                            is WebSocketSpeechDataSource.TranscriptionResult.Final -> {
                                val currentState = _transcriptionState.value
                                if (currentState is TranscriptionState.Recording) {
                                    _transcriptionState.value = currentState.copy(
                                        finalText = listOf(
                                            currentState.finalText,
                                            result.text
                                        ).filter { it.isNotBlank() }.joinToString(" "),
                                        partialText = ""
                                    )
                                }
                            }

                            is WebSocketSpeechDataSource.TranscriptionResult.SessionFinal -> {
                                val currentState = _transcriptionState.value
                                if (currentState is TranscriptionState.Recording) {
                                    _transcriptionState.value = TranscriptionState.Completed(
                                        text = result.text,
                                        languageCode = result.languageCode
                                    )
                                }
                            }

                            is WebSocketSpeechDataSource.TranscriptionResult.Error -> {
                                _transcriptionState.value = TranscriptionState.Error(result.message)
                            }
                        }
                    }
            } catch (e: Exception) {
                _transcriptionState.value = TranscriptionState.Error(e.message ?: "Unknown error")
            }
        }

    recordingJob = scope.launch {
            audioRecorder.startRecording { audioChunk ->
                scope.launch {
                    webSocketDataSource.streamAudioChunk(audioChunk)
                }
            }
        }
    }

    fun stopRecording() {
        recognitionJob?.cancel()
        recognitionJob = null

        scope.launch {
            audioRecorder.stopRecording()
            recordingJob?.cancel()
            recordingJob = null
            webSocketDataSource.stopStreamingRecognition()
            _transcriptionState.value = TranscriptionState.Idle
        }
    }

    fun reset() {
        _transcriptionState.value = TranscriptionState.Idle
        recognitionJob?.cancel()
        recordingJob?.cancel()
    }

    fun clear() {
        scope.cancel()
    }

    sealed class TranscriptionState {
        object Idle : TranscriptionState()
        data class Recording(
            val partialText: String,
            val finalText: String
        ) : TranscriptionState()

        data class Completed(
            val text: String,
            val languageCode: String?
        ) : TranscriptionState()

        data class Error(val message: String) : TranscriptionState()
    }
}


package com.mahbub.cloudespeechtotextstreeming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SpeechRecognitionViewModel(
    private val webSocketDataSource: WebSocketSpeechDataSource,
    private val audioRecorder: AudioRecorder
) : ViewModel() {

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState

    private var recognitionJob: Job? = null
    private var recordingJob: Job? = null

    fun startRecording(languageCode: String = "bn-IN") {
        if (_transcriptionState.value is TranscriptionState.Recording) {
            return
        }

        recognitionJob = viewModelScope.launch {
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

        recordingJob = viewModelScope.launch {
            audioRecorder.startRecording { audioChunk ->
                viewModelScope.launch {
                    webSocketDataSource.streamAudioChunk(audioChunk)
                }
            }
        }
    }

    fun stopRecording() {
        recognitionJob?.cancel()
        recognitionJob = null

        viewModelScope.launch {
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

    override fun onCleared() {
        super.onCleared()
        recognitionJob?.cancel()
        recordingJob?.cancel()
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

class SpeechRecognitionViewModelFactory(
    private val webSocketDataSource: WebSocketSpeechDataSource,
    private val audioRecorder: AudioRecorder
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeechRecognitionViewModel::class.java)) {
            return SpeechRecognitionViewModel(
                webSocketDataSource = webSocketDataSource,
                audioRecorder = audioRecorder
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

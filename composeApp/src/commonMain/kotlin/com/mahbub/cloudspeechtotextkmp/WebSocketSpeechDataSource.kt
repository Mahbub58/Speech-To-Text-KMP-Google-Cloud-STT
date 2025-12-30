package com.mahbub.cloudspeechtotextkmp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.close
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WebSocketSpeechDataSource(
    private val httpClient: HttpClient
) {

    sealed class TranscriptionResult {
        data class Partial(val text: String) : TranscriptionResult()
        data class Final(val text: String) : TranscriptionResult()
        data class SessionFinal(val text: String, val languageCode: String?) : TranscriptionResult()
        data class Error(val message: String) : TranscriptionResult()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var activeSession: WebSocketSession? = null

    @Serializable
    private data class CommandMessage(
        val type: String,
        val languageCode: String? = null
    )

    @Serializable
    private data class SttResponseMessage(
        val type: String,
        val text: String? = null,
        val message: String? = null,
        val languageCode: String? = null
    )

    suspend fun startStreamingRecognition(
        languageCode: String
    ): Flow<TranscriptionResult> = flow {
        val url = "wss://stt-ws.onrender.com/"

        httpClient.webSocket(urlString = url) {
            activeSession = this

            val startJson = json.encodeToString(
                CommandMessage.serializer(),
                CommandMessage(
                    type = "start",
                    languageCode = languageCode
                )
            )
            send(Frame.Text(startJson))

            for (frame in incoming) {
                val textFrame = frame as? Frame.Text ?: continue
                val payload = textFrame.readText()

                val message = json.decodeFromString(
                    SttResponseMessage.serializer(),
                    payload
                )

                when (message.type) {
                    "partial" -> {
                        message.text?.let {
                            emit(TranscriptionResult.Partial(it))
                        }
                    }

                    "final" -> {
                        message.text?.let {
                            emit(TranscriptionResult.Final(it))
                        }
                    }

                    "session_final" -> {
                        message.text?.let {
                            emit(TranscriptionResult.SessionFinal(it, message.languageCode))
                        }
                        break
                    }

                    "error" -> {
                        val errorMsg = message.message ?: "Remote STT error"
                        emit(TranscriptionResult.Error(errorMsg))
                        break
                    }
                }
            }
        }

        activeSession = null
    }

    suspend fun streamAudioChunk(audioChunk: ByteArray) {
        activeSession?.let { session ->
            session.send(Frame.Binary(fin = true, data = audioChunk))
        }
    }

    suspend fun stopStreamingRecognition() {
        activeSession?.let { session ->
            val stopJson = json.encodeToString(
                CommandMessage.serializer(),
                CommandMessage(type = "stop")
            )
            session.send(Frame.Text(stopJson))
            session.close()
            activeSession = null
        }
    }
}


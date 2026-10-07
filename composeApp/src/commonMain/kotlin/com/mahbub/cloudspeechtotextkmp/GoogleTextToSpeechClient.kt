package com.mahbub.cloudspeechtotextkmp

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
private data class GoogleTtsInput(val text: String)

@Serializable
private data class GoogleTtsVoice(
    val languageCode: String,
    val name: String? = null
)

@Serializable
private data class GoogleTtsAudioConfig(
    val audioEncoding: String,
    val speakingRate: Double? = null,
    val pitch: Double? = null
)

@Serializable
private data class GoogleTtsRequest(
    val input: GoogleTtsInput,
    val voice: GoogleTtsVoice,
    val audioConfig: GoogleTtsAudioConfig
)

@Serializable
private data class GoogleTtsResponse(
    val audioContent: String? = null
)

class GoogleTextToSpeechClient(
    private val httpClient: HttpClient
) {

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun synthesize(
        text: String,
        languageCode: String = "en-US",
        voiceName: String? = null,
        speakingRate: Double? = null,
        pitch: Double? = null
    ): ByteArray {
        if (text.isBlank()) {
            throw IllegalArgumentException("Text is empty")
        }

        val apiKey = getGoogleTtsApiKey()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("Google TTS API key is not configured")
        }

        val url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey"

        val requestBody = GoogleTtsRequest(
            input = GoogleTtsInput(text = text),
            voice = GoogleTtsVoice(
                languageCode = languageCode,
                name = voiceName
            ),
            audioConfig = GoogleTtsAudioConfig(
                audioEncoding = "MP3",
                speakingRate = speakingRate,
                pitch = pitch
            )
        )

        val httpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        if (!httpResponse.status.isSuccess()) {
            val errorBody = httpResponse.bodyAsText()
            throw IllegalStateException("TTS request failed: HTTP ${httpResponse.status.value} ${httpResponse.status.description} - $errorBody")

        }

        val response = httpResponse.body<GoogleTtsResponse>()

        val base64 = response.audioContent
            ?: throw IllegalStateException("No audio content returned from TTS service")

        return Base64.decode(base64)
    }
}

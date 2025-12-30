package com.mahbub.cloudspeechtotextkmp

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

actual fun createSpeechHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(WebSockets)
    }
}


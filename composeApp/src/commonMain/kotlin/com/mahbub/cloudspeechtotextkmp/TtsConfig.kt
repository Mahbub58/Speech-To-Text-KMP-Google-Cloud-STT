package com.mahbub.cloudspeechtotextkmp

fun getGoogleTtsApiKey(): String? = GOOGLE_TTS_API_KEY.ifBlank { null }

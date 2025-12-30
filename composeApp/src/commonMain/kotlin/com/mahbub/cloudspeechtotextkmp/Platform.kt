package com.mahbub.cloudspeechtotextkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
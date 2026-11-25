package com.example.androidpackapp

class AiConnectionException(message: String) : Exception(message)

fun assistantError(status: Int, code: String): String = when {
    code == "provider_429" -> "The free AI service has reached its usage limit. Please try again later."
    status == 429 -> "Too many questions in a short time. Please wait a minute before retrying."
    code == "timeout" || code == "provider_408" -> "The AI provider took too long to respond. Tap Retry to try your question again."
    code == "incomplete" -> "The AI provider returned an incomplete answer. Tap Retry to try again."
    code in listOf("provider_401", "provider_402", "provider_403") -> "The AI service needs attention from the app owner. Please try again later."
    else -> "The AI provider is temporarily busy. Tap Retry to try your question again."
}

// Kept as an internal diagnostics helper; end users never configure provider credentials.
fun routerError(status: Int): String = when (status) {
    401, 403 -> "OpenRouter rejected this key or model access. Check your key and account privacy settings."
    402 -> "OpenRouter requires credits for this request. Check your account."
    404 -> "This OpenRouter model is unavailable."
    429 -> "OpenRouter's free-model limit was reached. Please wait and try again."
    else -> "OpenRouter is unavailable (HTTP $status). Please try again."
}

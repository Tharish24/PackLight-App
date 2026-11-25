package com.example.androidpackapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class Weather(val temperature: Double, val code: Int, val observed: String, val fetched: String)
data class ChatMessage(val text: String, val user: Boolean)
class TravelViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = TravelRepository(app)
    val state = MutableStateFlow(TravelState())
    val ready = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val weather = MutableStateFlow<Map<String, Weather>>(emptyMap())
    val weatherStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _messages = MutableStateFlow(listOf(ChatMessage("Hello, traveler. Choose any country or name it in your question. Ask for travel tips, packing advice, food, culture or an itinerary.", false)))
    val messages: StateFlow<List<ChatMessage>> = _messages
    val thinking = MutableStateFlow(false)
    val canRetry = MutableStateFlow(false)
    private var failedQuestion: Pair<String, String>? = null
    fun retryLastQuestion() {
        failedQuestion?.let { askInternal(it.first, it.second, false) }
    }
    init {
        viewModelScope.launch {
            // Remove the retired per-user credential without sending it anywhere.
            withContext(Dispatchers.IO) {
                android.util.AtomicFile(java.io.File(app.noBackupFilesDir, "openrouter-key.enc")).delete()
            }
        }
        viewModelScope.launch {
            try { repository.state.collect { state.value = it; ready.value = true } }
            catch (_: Exception) { error.value = "Saved data could not be read. Please restart the app. No data has been overwritten." }
        }
    }
    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.value = "Could not save changes. Check available storage and try again." }
        }
    }
    fun save(trip: Trip) = change { repository.saveTrip(trip) }
    fun delete(id: String) = change { repository.deleteTrip(id) }
    fun best(score: Int) = change { repository.highScore(score) }
    fun refresh(destination: Destination) {
        if (weatherStatus.value[destination.city] == "Updating…") return
        weatherStatus.value = weatherStatus.value + (destination.city to "Updating…")
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val body = request("https://api.open-meteo.com/v1/forecast?latitude=${destination.lat}&longitude=${destination.lon}&current=temperature_2m,weather_code&timezone=auto")
                    val current = JSONObject(body).getJSONObject("current")
                    Weather(current.getDouble("temperature_2m"), current.getInt("weather_code"), current.getString("time"), Instant.now().toString())
                }
                weather.value = weather.value + (destination.city to result)
                weatherStatus.value = weatherStatus.value + (destination.city to "")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { weatherStatus.value = weatherStatus.value + (destination.city to "Weather unavailable. Check your connection and retry. Previous readings may be stale.") }
        }
    }
    fun ask(prompt: String, city: String) = askInternal(prompt, city, true)
    private fun askInternal(prompt: String, city: String, appendUser: Boolean) {
        if (thinking.value || prompt.isBlank()) return
        if (appendUser) _messages.value += ChatMessage(prompt, true)
        canRetry.value = false
        thinking.value = true
        viewModelScope.launch {
            try {
                val answer = if (BuildConfig.AI_URL.isBlank()) offlineAnswer(prompt, city) else withContext(Dispatchers.IO) {
                    require(BuildConfig.AI_URL.startsWith("https://"))
                    val destinationName = mentionedCountry(prompt)?.name ?: city
                    val response = JSONObject(request(BuildConfig.AI_URL, JSONObject().put("message", prompt).put("destination", destinationName).toString()))
                    response.getString("reply").also {
                        if (it.isBlank()) throw AiConnectionException("The AI provider returned an empty answer. Tap Retry.")
                    }
                }
                _messages.value += ChatMessage(answer, false)
                failedQuestion = null
            } catch (e: CancellationException) { throw e }
            catch (e: AiConnectionException) {
                _messages.value += ChatMessage(e.message ?: assistantError(503, ""), false)
                failedQuestion = prompt to city; canRetry.value = true
            }
            catch (_: Exception) {
                _messages.value += ChatMessage("Could not reach the travel assistant. Check your internet connection, then tap Retry.", false)
                failedQuestion = prompt to city; canRetry.value = true
            }
            finally { thinking.value = false }
        }
    }
    private fun offlineAnswer(prompt: String, city: String): String {
        val country = mentionedCountry(prompt) ?: countries.firstOrNull { it.name == city }
        val d = country?.let(::countryGuide)
            ?: destinations.firstOrNull { it.city.equals(city, true) && country == null }
        if (d == null) return country?.let { offlineCountryAnswer(prompt, it) }
            ?: "Choose a country from the suggestions so I can help plan your trip."
        val p = prompt.lowercase()
        val answer = when {
            listOf("pack", "bring", "luggage", "checklist").any { it in p } -> "Start with documents, medication, chargers, comfortable shoes and reusable layers. Add a rain shell and a local adapter. Save your checklist in Pack. " + d.climate
            listOf("food", "eat", "restaurant").any { it in p } -> d.food
            listOf("transport", "train", "bus", "around").any { it in p } -> d.transport
            listOf("culture", "etiquette", "custom").any { it in p } -> d.culture
            listOf("currency", "money", "cash").any { it in p } -> d.currency
            listOf("climate", "season", "weather").any { it in p } -> d.climate + " Open Explore for a live weather update."
            listOf("visit", "see", "attraction", "plan", "day").any { it in p } -> d.attractions + " Allow time for rest and travel between stops."
            else -> "I can help with packing, attractions, culture, food, transport, currency and seasonal climate. Try “What should I pack for ${d.city}?” A generative AI backend has not been configured."
        }
        return "Offline guide · ${d.city}\n\n$answer"
    }
}
private fun request(address: String, body: String? = null): String {
    val connection = URL(address).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 12000; connection.readTimeout = if (body != null) 90000 else 15000
        connection.instanceFollowRedirects = false
        if (body != null) {
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        if (connection.responseCode !in 200..299) {
            val code = runCatching {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText().take(4096) } ?: "{}"
                JSONObject(errorBody).optString("code")
            }.getOrDefault("")
            throw AiConnectionException(assistantError(connection.responseCode, code))
        }
        return connection.inputStream.bufferedReader().use { it.readText().also { text -> check(text.length <= 100000) } }
    } finally { connection.disconnect() }
}

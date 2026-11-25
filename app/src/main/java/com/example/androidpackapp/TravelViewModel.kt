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
import java.text.Normalizer
import java.time.Instant

data class Weather(val temperature: Double, val code: Int, val observed: String, val fetched: String)
data class ChatMessage(val text: String, val user: Boolean)
object ContentPolicy {
    private val prohibited = Regex("\\b(fuck\\w*|shit\\w*|bitch\\w*|asshole\\w*|nigger\\w*|faggot\\w*|porn\\w*|rape\\w*|suicid\\w*|self.?harm|kill\\w*|murder\\w*|bomb\\w*|weapon\\w*|terror\\w*|cocaine|heroin|methamphetamine|hate|nazi\\w*)\\b")
    fun allowed(text: String): Boolean {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase()
            .replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('@', 'a').replace('$', 's')
        return text.length in 1..2000 && !prohibited.containsMatchIn(normalized) &&
            !prohibited.containsMatchIn(normalized.replace(Regex("[^a-z ]"), ""))
    }
}
class TravelViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = TravelRepository(app)
    val state = MutableStateFlow(TravelState())
    val ready = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val weather = MutableStateFlow<Map<String, Weather>>(emptyMap())
    val weatherStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _messages = MutableStateFlow(listOf(ChatMessage("Hello, traveler. Search for any country above, or name it in your question. Offline mode offers general travel help and four featured city guides.", false)))
    val messages: StateFlow<List<ChatMessage>> = _messages
    val thinking = MutableStateFlow(false)
    init {
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
    fun ask(prompt: String, city: String) {
        if (thinking.value || prompt.isBlank()) return
        if (!ContentPolicy.allowed(prompt)) {
            _messages.value += ChatMessage("Please keep requests respectful and travel-related. I cannot help with harmful, hateful, explicit or offensive content.", false)
            return
        }
        _messages.value += ChatMessage(prompt, true)
        thinking.value = true
        viewModelScope.launch {
            try {
                val answer = if (BuildConfig.AI_URL.isBlank()) offlineAnswer(prompt, city) else withContext(Dispatchers.IO) {
                    require(BuildConfig.AI_URL.startsWith("https://"))
                    val destinationName = mentionedCountry(prompt)?.name ?: city
                    val response = JSONObject(request(BuildConfig.AI_URL, JSONObject().put("message", prompt).put("destination", destinationName).toString()))
                    // Server must moderate input and output before returning safe=true.
                    if (!response.optBoolean("safe", false)) "This request or response was blocked by the safety filter."
                    else response.getString("reply").let { if (ContentPolicy.allowed(it)) it else "The response was blocked by the language filter." }
                }
                _messages.value += ChatMessage(answer, false)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _messages.value += ChatMessage("The AI service is unavailable. Please try again. Offline guide: " + offlineAnswer(prompt, city), false) }
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
        connection.connectTimeout = 12000; connection.readTimeout = 65000
        connection.instanceFollowRedirects = false
        if (body != null) {
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        check(connection.responseCode in 200..299)
        return connection.inputStream.bufferedReader().use { it.readText().also { text -> check(text.length <= 100000) } }
    } finally { connection.disconnect() }
}

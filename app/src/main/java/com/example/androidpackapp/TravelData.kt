package com.example.androidpackapp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class PackingItem(val id: String = UUID.randomUUID().toString(), val name: String, val packed: Boolean = false)
data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val destination: String,
    val start: String,
    val end: String,
    val notes: String = "",
    val itinerary: List<String> = emptyList(),
    val packing: List<PackingItem> = essentials.map { PackingItem(name = it) }
)
val essentials = listOf("Passport & travel documents", "Phone & charger", "Travel adapter", "Everyday outfits", "Comfortable shoes", "Toiletries", "Personal medication", "Reusable water bottle")
data class TravelState(val trips: List<Trip> = emptyList(), val highScore: Int = 0)
private val Context.packStore by preferencesDataStore("packlight")
class TravelRepository(context: Context) {
    private val store = context.applicationContext.packStore
    private val key = stringPreferencesKey("travel_v1")
    val state = store.data.map { decode(it[key] ?: "{}") }
    suspend fun saveTrip(trip: Trip) = update { old -> old.copy(trips = old.trips.filterNot { it.id == trip.id } + trip) }
    suspend fun deleteTrip(id: String) = update { it.copy(trips = it.trips.filterNot { t -> t.id == id }) }
    suspend fun highScore(score: Int) = update { it.copy(highScore = maxOf(score, it.highScore)) }
    private suspend fun update(change: (TravelState) -> TravelState) {
        store.edit { prefs -> prefs[key] = encode(change(decode(prefs[key] ?: "{}"))) }
    }
    private fun encode(state: TravelState): String = JSONObject().apply {
        put("highScore", state.highScore)
        put("trips", JSONArray().apply { state.trips.forEach { trip ->
            put(JSONObject().apply {
                put("id", trip.id); put("destination", trip.destination); put("start", trip.start); put("end", trip.end)
                put("notes", trip.notes); put("itinerary", JSONArray(trip.itinerary))
                put("packing", JSONArray().apply { trip.packing.forEach { item ->
                    put(JSONObject().put("id", item.id).put("name", item.name).put("packed", item.packed))
                } })
            })
        } })
    }.toString()
    private fun decode(raw: String): TravelState {
        val root = JSONObject(raw)
        val array = root.optJSONArray("trips") ?: JSONArray()
        return TravelState((0 until array.length()).map { i ->
            val t = array.getJSONObject(i)
            val list = t.optJSONArray("itinerary") ?: JSONArray()
            val packing = t.optJSONArray("packing") ?: JSONArray()
            Trip(t.getString("id"), t.getString("destination"), t.getString("start"), t.getString("end"),
                t.optString("notes"), (0 until list.length()).map { list.getString(it) },
                (0 until packing.length()).map { n -> packing.getJSONObject(n).let {
                    PackingItem(it.getString("id"), it.getString("name"), it.getBoolean("packed"))
                } })
        }, root.optInt("highScore"))
    }
}
fun validDates(start: String, end: String): Boolean = runCatching {
    !LocalDate.parse(end).isBefore(LocalDate.parse(start))
}.getOrDefault(false)

data class Destination(
    val city: String, val country: String, val caption: String, val lat: Double, val lon: Double,
    val attractions: String, val culture: String, val food: String, val transport: String,
    val currency: String, val climate: String, val color: Long
)
val destinations = listOf(
    Destination("Kyoto", "Japan", "Quiet lanes. Extraordinary discoveries.", 35.0116, 135.7681,
        "Walk the Fushimi Inari shrine paths early; explore Kiyomizu-dera and the Arashiyama bamboo grove. Check official opening hours before visiting.",
        "Bow politely, keep voices low on public transport and remove shoes where indicated. Respect photography restrictions in Gion.",
        "Try yudofu, matcha sweets and Kyoto-style kaiseki. Ask about fish stock if you avoid seafood.",
        "Use trains for longer journeys and buses for temple districts. An IC card simplifies local fares.",
        "Japanese yen · JPY. Carry some cash for small shops and temples.",
        "Spring: 8–22°C, blossoms and crowds. Summer: 22–34°C, humid and rainy. Autumn: 10–25°C, colorful leaves. Winter: 0–10°C, cold mornings.", 0xFFD9E8CF),
    Destination("Lisbon", "Portugal", "Follow the light to the Atlantic.", 38.7223, -9.1393,
        "Explore Alfama, Belém Tower and the Jerónimos Monastery. Book popular monuments ahead.",
        "Greet with olá. Listen quietly during fado performances and be considerate in residential lanes.",
        "Pastéis de nata, grilled sardines and caldo verde are local favorites. Check allergens with the kitchen.",
        "Metro, buses and trams connect the city. Wear shoes with grip for steep cobbled streets.",
        "Euro · EUR. Cards are common; keep a little cash for small purchases.",
        "Spring: 12–22°C. Summer: 18–29°C, mostly dry. Autumn: 14–25°C, increasing rain. Winter: 8–16°C, wet and mild.", 0xFFF4D6BA),
    Destination("Dubai", "United Arab Emirates", "Desert horizons. A city of possibilities.", 25.2048, 55.2708,
        "Visit Al Fahidi, the Dubai Creek waterfront and the Museum of the Future. Reserve timed attractions ahead.",
        "Dress respectfully in traditional areas and religious sites. Ask permission before photographing people.",
        "Try machboos, luqaimat and Emirati coffee. International and vegetarian choices are widely available.",
        "Metro connects major districts. Use licensed taxis for the last mile and avoid long summer walks.",
        "UAE dirham · AED. Cards are widely accepted.",
        "Winter: 14–26°C, pleasant outdoors. Spring: 20–35°C. Summer: 30–43°C, extreme heat. Autumn: 23–36°C. Carry water and plan indoor breaks.", 0xFFECDAB8),
    Destination("Reykjavík", "Iceland", "A small capital. A vast adventure.", 64.1466, -21.9426,
        "Visit Hallgrímskirkja, Harpa and the old harbor. Check road and weather alerts before trips beyond the city.",
        "Shower thoroughly before entering public pools. Stay on marked paths and protect fragile landscapes.",
        "Try rye bread, local fish and skyr. Vegetarian options are available in the capital.",
        "Walk central districts or use Strætó buses. Rural travel requires planning; conditions can change rapidly.",
        "Icelandic króna · ISK. Cards are widely accepted.",
        "Winter: −3–4°C, short daylight. Spring: 0–9°C. Summer: 8–15°C, long daylight. Autumn: 1–10°C. Wind and rain are possible all year.", 0xFFCCDDE9)
)

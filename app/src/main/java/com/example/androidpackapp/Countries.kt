package com.example.androidpackapp

import java.text.Normalizer
import java.util.Locale

data class Country(val code: String, val name: String, val aliases: List<String> = emptyList())
private fun searchText(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim()

// ISO 3166 regions include countries and dependent territories. Kosovo is also selectable.
val countries: List<Country> = (Locale.getISOCountries().toList() + "XK").distinct().map { code ->
    val name = when (code) {
        "XK" -> "Kosovo"
        else -> Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH)
    }
    val aliases = when (code) {
        "US" -> listOf("USA", "United States of America", "America")
        "GB" -> listOf("UK", "Great Britain", "Britain", "England", "Scotland", "Wales", "Northern Ireland")
        "AE" -> listOf("UAE", "Emirates")
        "CN" -> listOf("China", "People's Republic of China")
        "KR" -> listOf("South Korea", "Republic of Korea")
        "KP" -> listOf("North Korea")
        "CI" -> listOf("Ivory Coast")
        "CZ" -> listOf("Czech Republic", "Czechia")
        "VA" -> listOf("Vatican City", "Holy See")
        "PS" -> listOf("Palestine", "Palestinian Territories")
        "TR" -> listOf("Turkey", "Türkiye")
        "SZ" -> listOf("Swaziland", "Eswatini")
        "TL" -> listOf("East Timor", "Timor Leste")
        "MM" -> listOf("Burma", "Myanmar")
        "CV" -> listOf("Cape Verde", "Cabo Verde")
        else -> emptyList()
    }
    Country(code, name, aliases)
}.sortedBy { searchText(it.name) }

fun suggestCountries(query: String): List<Country> {
    val key = searchText(query)
    if (key.isEmpty()) return countries
    return countries.filter { c ->
        c.code.equals(query.trim(), true) || (listOf(c.name) + c.aliases).any { searchText(it).contains(key) }
    }.sortedWith(compareBy<Country> {
        if ((listOf(it.name) + it.aliases).any { name -> searchText(name).startsWith(key) }) 0 else 1
    }.thenBy { searchText(it.name) })
}

fun mentionedCountry(prompt: String): Country? {
    val text = " " + searchText(prompt) + " "
    return countries.flatMap { c -> (listOf(c.name) + c.aliases).map { c to searchText(it) } }
        .filter { (_, name) -> name.length >= 3 && text.contains(" $name ") }
        .maxByOrNull { it.second.length }?.first
}

fun countryGuide(country: Country): Destination? = when (country.code) {
    "JP" -> destinations.first { it.city == "Kyoto" }
    "PT" -> destinations.first { it.city == "Lisbon" }
    "AE" -> destinations.first { it.city == "Dubai" }
    "IS" -> destinations.first { it.city == "Reykjavík" }
    else -> null
}

fun offlineCountryAnswer(prompt: String, country: Country): String {
    val topic = searchText(prompt)
    val body = if (listOf("pack", "bring", "luggage", "checklist").any { it in topic })
        "Start with travel documents, personal medication, chargers and comfortable shoes. Check entry requirements and the forecast for your exact city and dates, then add suitable layers and a local adapter. Save your checklist in Pack."
    else "You can plan a trip to ${country.name} and save its packing list. A detailed offline guide for this country is not bundled yet. Check official tourism and entry information for your exact destination and dates. Connect the AI backend for country-specific planning, food, culture and transport advice."
    return "Offline travel helper · ${country.name}\n\n$body"
}

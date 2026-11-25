package com.example.androidpackapp

import org.junit.Assert.*
import org.junit.Test

class CountrySearchTest {
    @Test fun worldwideCatalogueIncludesCountriesAndTerritories() {
        assertTrue(countries.size >= 249)
        listOf("CN", "IN", "BR", "ZA", "US", "GB", "VA", "PS", "XK").forEach { code ->
            assertTrue("Missing $code", countries.any { it.code == code })
        }
        assertEquals(countries.size, countries.map { it.code }.distinct().size)
    }
    @Test fun lettersAndAliasesSuggestTheRightCountries() {
        assertTrue(suggestCountries("Chi").any { it.code == "CN" })
        assertEquals("AE", suggestCountries("UAE").single().code)
        assertTrue(suggestCountries("UK").any { it.code == "GB" })
        assertTrue(suggestCountries("ivory").any { it.code == "CI" })
        assertTrue(suggestCountries("turkiye").any { it.code == "TR" })
        assertTrue(suggestCountries("zzzzzz").isEmpty())
        assertEquals(countries, suggestCountries(""))
    }
    @Test fun countryInQuestionOverridesPreviousDestinationWithoutSubstringMatches() {
        assertEquals("CN", mentionedCountry("I WANT TO GO China")?.code)
        assertEquals("KR", mentionedCountry("I want to visit South Korea")?.code)
        assertNull(mentionedCountry("Which island is sunny?"))
    }
    @Test fun unsupportedOfflineCountryDoesNotFallBackToKyoto() {
        val china = countries.first { it.code == "CN" }
        val answer = offlineCountryAnswer("I want to go China", china)
        assertTrue(answer.contains(china.name))
        assertFalse(answer.contains("Kyoto"))
        assertTrue(answer.contains("not bundled"))
    }
}

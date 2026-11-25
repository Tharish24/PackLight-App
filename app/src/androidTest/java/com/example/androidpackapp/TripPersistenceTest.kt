package com.example.androidpackapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripPersistenceTest {
    @Test fun tripItineraryAndChecklistRoundTripThroughStorage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = TravelRepository(context)
        val trip = Trip(destination = "Persistence test", start = "2027-01-01", end = "2027-01-03",
            notes = "Reservation 123", itinerary = listOf("Day 1: Old town"),
            packing = listOf(PackingItem(name = "Passport", packed = true)))
        try {
            repository.saveTrip(trip)
            val restored = TravelRepository(context).state.first().trips.first { it.id == trip.id }
            assertEquals(trip, restored)
            val changed = restored.copy(packing = restored.packing.map { it.copy(packed = false) })
            repository.saveTrip(changed)
            assertFalse(repository.state.first().trips.first { it.id == trip.id }.packing.single().packed)
        } finally {
            repository.deleteTrip(trip.id)
        }
    }
}

package com.example.androidpackapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent { PackLightTheme { PackLightApp() } }
    }
}
private val Pine = Color(0xFF18594B)
@Composable
fun PackLightTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFA3D5BF), secondary = Color(0xFFE6BC91))
    else lightColorScheme(primary = Pine, secondary = Color(0xFF815331), background = Color(0xFFFAF9F5),
        surface = Color(0xFFFAF9F5), surfaceContainer = Color(0xFFF0F0E8),
        primaryContainer = Color(0xFFD9ECDD), onPrimaryContainer = Color(0xFF153C30),
        secondaryContainer = Color(0xFFF0DFC8), onSecondaryContainer = Color(0xFF382B1D),
        surfaceContainerHighest = Color(0xFFE9EDE4))
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
private val tabs = listOf("Trips", "Explore", "Pack", "Assistant", "Play")
private val tabIcons = listOf(Icons.Default.Luggage, Icons.Default.Explore, Icons.Default.Checklist, Icons.AutoMirrored.Filled.Chat, Icons.Default.SportsEsports)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackLightApp(vm: TravelViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(tab != 0 && !editor) { tab = 0 }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Row {
            if (wide) NavigationRail(Modifier.fillMaxHeight()) {
                Spacer(Modifier.height(24.dp))
                tabs.forEachIndexed { i, title -> NavigationRailItem(selected = tab == i, onClick = { tab = i },
                    icon = { Icon(tabIcons[i], title) }, label = { Text(title) }) }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlightTakeoff, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp)); Text("PackLight", fontWeight = FontWeight.Bold)
                } }, actions = { if (wide) Text("GO FURTHER. CARRY LESS.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 16.dp)) }) },
                bottomBar = { if (!wide) NavigationBar { tabs.forEachIndexed { i, title ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Icon(tabIcons[i], null) }, label = { Text(title) })
                } } }
            ) { padding ->
                Box(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    if (!ready) Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (error == null) CircularProgressIndicator() else Text(error!!)
                    } else AnimatedContent(targetState = tab, label = "section") { page ->
                        Box(Modifier.widthIn(max = 1000.dp).fillMaxSize()) {
                            when (page) {
                                0 -> TripsScreen(state, { editingId = null; editor = true }, { editingId = it.id; editor = true }, vm::delete)
                                1 -> ExploreScreen(vm)
                                2 -> PackingScreen(state, vm::save)
                                3 -> AssistantScreen(vm)
                                else -> GameScreen(state.highScore, vm::best)
                            }
                        }
                    }
                }
            }
        }
    }
    if (editor) TripEditor(state.trips.firstOrNull { it.id == editingId }, {
        vm.save(it); editor = false
    }, { editor = false })
    if (error != null && ready) AlertDialog(onDismissRequest = { vm.error.value = null }, title = { Text("Unable to save") },
        text = { Text(error!!) }, confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("OK") } })
}

@Composable
private fun Heading(eyebrow: String, title: String, subtitle: String) {
    Text(eyebrow.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
}
@Composable
private fun SectionCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
@Composable
private fun TripsScreen(state: TravelState, add: () -> Unit, edit: (Trip) -> Unit, delete: (String) -> Unit) {
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Heading("Your next chapter", "Less baggage.\nMore possibility.", "A little planning makes room for a lot of adventure.") }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Pine, contentColor = Color.White), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Public, null, modifier = Modifier.size(40.dp))
                    Text("The world is waiting.", style = MaterialTheme.typography.headlineMedium)
                    Text("Bring your plans together, then pack only what matters.")
                    FilledTonalButton(onClick = add) { Icon(Icons.Default.Add, null); Text(" Plan a trip") }
                }
            }
        }
        item { Text("Your trips · ${state.trips.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.trips.isEmpty()) item { SectionCard("A fresh itinerary", "Start with a destination and dates. Your trips and packing lists stay on this device, even offline.") }
        items(state.trips.sortedBy { it.start }, key = { it.id }) { trip ->
            Card(onClick = { edit(trip) }, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trip.destination, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${trip.start}  →  ${trip.end}")
                    Text("${trip.itinerary.size} plans · ${trip.packing.count { it.packed }}/${trip.packing.size} packed", color = MaterialTheme.colorScheme.primary)
                    if (trip.notes.isNotBlank()) Text(trip.notes)
                    Row {
                        TextButton(onClick = { edit(trip) }) { Text("Edit itinerary") }
                        TextButton(onClick = { deleting = trip.id }) { Text("Delete") }
                    }
                }
            }
        }
        item { Text("Made for the journey, online or off.", style = MaterialTheme.typography.labelMedium) }
    }
    if (deleting != null) AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this trip?") },
        text = { Text("Its itinerary and packing list will also be removed.") },
        confirmButton = { TextButton(onClick = { delete(deleting!!); deleting = null }) { Text("Delete trip") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep trip") } })
}

@Composable
private fun TripEditor(trip: Trip?, save: (Trip) -> Unit, dismiss: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf(trip?.destination ?: "") }
    var start by rememberSaveable { mutableStateOf(trip?.start ?: LocalDate.now().toString()) }
    var end by rememberSaveable { mutableStateOf(trip?.end ?: LocalDate.now().plusDays(3).toString()) }
    var notes by rememberSaveable { mutableStateOf(trip?.notes ?: "") }
    var plans by rememberSaveable { mutableStateOf(trip?.itinerary?.joinToString("\n") ?: "") }
    val valid = destination.isNotBlank() && validDates(start, end)
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (trip == null) "Plan something wonderful" else "Your itinerary") },
        text = { Column(Modifier.heightIn(max = 500.dp).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CountrySearchField(destination, { destination = it.take(100) }, label = "Destination · country or city")
            OutlinedTextField(start, { start = it.take(10) }, label = { Text("Start · YYYY-MM-DD") }, singleLine = true)
            OutlinedTextField(end, { end = it.take(10) }, label = { Text("End · YYYY-MM-DD") }, singleLine = true,
                isError = !validDates(start, end), supportingText = { Text("Use valid dates; end must follow start.") })
            OutlinedTextField(plans, { plans = it.take(6000) }, label = { Text("Itinerary · one plan per line") }, minLines = 3)
            OutlinedTextField(notes, { notes = it.take(2000) }, label = { Text("Notes & reservations") }, minLines = 2)
        } },
        confirmButton = { TextButton(enabled = valid, onClick = {
            save((trip ?: Trip(destination = destination.trim(), start = start, end = end)).copy(
                destination = destination.trim(), start = start, end = end, notes = notes,
                itinerary = plans.lines().filter { it.isNotBlank() }))
        }) { Text("Save trip") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun TripPicker(trips: List<Trip>, selected: String?, choose: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(trips.firstOrNull { it.id == selected }?.destination ?: "Choose a trip"); Icon(Icons.Default.ExpandMore, null) }
        DropdownMenu(expanded, { expanded = false }) {
            trips.forEach { trip -> DropdownMenuItem(text = { Text("${trip.destination} · ${trip.start}") }, onClick = { choose(trip.id); expanded = false }) }
        }
    }
}
@Composable
private fun PackingScreen(state: TravelState, save: (Trip) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val trip = state.trips.firstOrNull { it.id == selected } ?: state.trips.firstOrNull()
    var newItem by rememberSaveable(trip?.id) { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Heading("Carry less", "Ready, set, pack.", "Saved checklists for every kind of escape.") }
        if (trip == null) item { SectionCard("Your checklist starts with a trip", "Create a trip in Trips to get an essentials list. You can add your own items and check them off here.") }
        else {
            item { TripPicker(state.trips, trip.id) { selected = it } }
            item {
                val count = trip.packing.count { it.packed }
                Text("$count of ${trip.packing.size} essentials packed", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { if (trip.packing.isEmpty()) 0f else count.toFloat() / trip.packing.size }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(newItem, { newItem = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Add a packing item") }, singleLine = true,
                    trailingIcon = { IconButton(enabled = newItem.isNotBlank(), onClick = {
                        save(trip.copy(packing = trip.packing + PackingItem(name = newItem.trim()))); newItem = ""
                    }) { Icon(Icons.Default.Add, "Add item") } })
            }
            items(trip.packing, key = { it.id }) { item ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(item.packed, { checked -> save(trip.copy(packing = trip.packing.map { if (it.id == item.id) it.copy(packed = checked) else it })) }, modifier = Modifier.semantics { contentDescription = item.name })
                        Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { save(trip.copy(packing = trip.packing.filterNot { it.id == item.id })) }) { Icon(Icons.Default.Close, "Remove ${item.name}") }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySearchField(value: String, change: (String) -> Unit, label: String = "Search country") {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value) { suggestCountries(value) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = { change(it); expanded = true },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            label = { Text(label) }, placeholder = { Text("Type a country name, e.g. China") },
            singleLine = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)) {
            if (matches.isEmpty()) DropdownMenuItem(
                text = { Text("No matching country") }, onClick = {}, enabled = false
            )
            matches.forEach { country ->
                DropdownMenuItem(text = { Text(country.name) },
                    onClick = { change(country.name); expanded = false })
            }
        }
    }
}
@Composable
private fun ExploreScreen(vm: TravelViewModel) {
    var name by rememberSaveable { mutableStateOf("Japan") }
    val country = countries.firstOrNull { it.name == name }
    val guide = country?.let(::countryGuide)
    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading("A sense of place", "Find your elsewhere.", "Search countries and territories worldwide.")
            CountrySearchField(name, { name = it })
        }
        if (guide != null) Box(Modifier.weight(1f)) { CuratedGuideScreen(vm, guide.city) }
        else Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            SectionCard(if (country == null) "Choose a country" else country.name,
                if (country == null) "Type letters above, then choose a suggestion."
                else "You can plan a trip and ask the assistant about this country. A detailed offline guide is not bundled yet. Live weather is currently available for the four featured city guides.")
        }
    }
}
@Composable
private fun CuratedGuideScreen(vm: TravelViewModel, city: String) {
    val destination = destinations.first { it.city == city }
    val weather by vm.weather.collectAsStateWithLifecycle()
    val statuses by vm.weatherStatus.collectAsStateWithLifecycle()
    LaunchedEffect(city) { if (weather[city] == null) vm.refresh(destination) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Featured city guide · $city", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(destination.color), contentColor = Color(0xFF1B332C)), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Landscape, null, Modifier.size(64.dp))
                    Text(destination.country.uppercase(), style = MaterialTheme.typography.labelLarge)
                    Text(city, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text(destination.caption, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current weather", style = MaterialTheme.typography.titleLarge)
                    val w = weather[city]
                    if (w != null) {
                        Text("${w.temperature}°C · ${weatherLabel(w.code)}", style = MaterialTheme.typography.headlineSmall)
                        Text("Observed: ${w.observed} · destination local time", style = MaterialTheme.typography.bodySmall)
                        Text("Updated: ${w.fetched} · UTC", style = MaterialTheme.typography.bodySmall)
                    } else Text("No live reading yet.")
                    if (!statuses[city].isNullOrBlank()) Text(statuses[city]!!)
                    TextButton(onClick = { vm.refresh(destination) }, enabled = statuses[city] != "Updating…") { Text("Refresh weather") }
                    Text("Weather: Open-Meteo · model-based current conditions", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { SectionCard("Worth the detour", destination.attractions) }
        item { SectionCard("Culture & courtesy", destination.culture) }
        item { SectionCard("At the table", destination.food) }
        item { SectionCard("Getting around", destination.transport) }
        item { SectionCard("Currency", destination.currency) }
        item { SectionCard("Through the seasons", destination.climate) }
        item { Text("Offline editorial guides · approximate seasonal ranges, not forecasts. Verify schedules, prices and travel advisories before departure.", style = MaterialTheme.typography.bodySmall) }
    }
}
private fun weatherLabel(code: Int) = when (code) { 0 -> "Clear"; in 1..3 -> "Cloudy"; 45, 48 -> "Fog"; in 51..67 -> "Rain"; in 71..77 -> "Snow"; in 80..82 -> "Showers"; 85, 86 -> "Snow showers"; in 95..99 -> "Thunderstorms"; else -> "Conditions code $code" }

@Composable
private fun AssistantScreen(vm: TravelViewModel) {
    var countryName by rememberSaveable { mutableStateOf("") }
    val country = countries.firstOrNull { it.name == countryName }
    var prompt by rememberSaveable { mutableStateOf("") }
    val messages by vm.messages.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Heading("Your travel companion", "A little local insight.", if (BuildConfig.AI_URL.isBlank()) "Offline guide mode · AI backend not configured" else "AI mode · messages sent to your configured service")
        CountrySearchField(countryName, { countryName = it })
        Text(if (country == null) "Choose a country suggestion, or name a country in your question." else "Destination: ${country.name}", style = MaterialTheme.typography.bodySmall)
        Text("Respectful travel questions only. Harmful and offensive content is restricted. Never share passwords or sensitive documents.", style = MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f), reverseLayout = true, contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (thinking) item { Text("Thinking…", color = MaterialTheme.colorScheme.primary) }
            items(messages.asReversed()) { message ->
                Card(colors = CardDefaults.cardColors(containerColor = if (message.user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(if (message.user) "YOU" else "PACKLIGHT", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp)); Text(message.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        OutlinedTextField(prompt, { prompt = it.take(2000) }, Modifier.fillMaxWidth().padding(bottom = 12.dp), label = { Text("Ask a travel question") },
            maxLines = 4, trailingIcon = { IconButton(enabled = !thinking && prompt.isNotBlank() && (country != null || mentionedCountry(prompt) != null), onClick = { val target = mentionedCountry(prompt) ?: country; if (target != null) { countryName = target.name; vm.ask(prompt.trim(), target.name); prompt = "" } }) { Icon(Icons.Default.Send, "Send question") } })
    }
}
@Preview(showBackground = true, widthDp = 390, heightDp = 840)
@Preview(showBackground = true, widthDp = 900, heightDp = 800)
@Composable
fun TripsPreview() { PackLightTheme { Surface { TripsScreen(TravelState(), {}, {}, {}) } } }

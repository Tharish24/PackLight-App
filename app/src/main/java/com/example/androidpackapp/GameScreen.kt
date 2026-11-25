package com.example.androidpackapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.random.Random

fun gameSpeed(score: Int): Float = (0.012f + score * 0.0005f).coerceAtMost(0.045f)
fun hitsPlayer(obstacleLane: Int, playerLane: Int, previous: Float, next: Float): Boolean =
    obstacleLane == playerLane && previous <= 0.88f && next >= 0.72f

@Composable
fun GameScreen(best: Int, onBest: (Int) -> Unit) {
    var lane by rememberSaveable { mutableIntStateOf(1) }
    var hazard by rememberSaveable { mutableIntStateOf(0) }
    var progress by rememberSaveable { mutableFloatStateOf(-0.15f) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(false) }
    var over by rememberSaveable { mutableStateOf(false) }
    val latestBest by rememberUpdatedState(onBest)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) running = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Returning to the tab always requires an explicit resume.
    DisposableEffect(Unit) { running = false; onDispose { running = false } }
    LaunchedEffect(running) {
        while (running) {
            delay(40)
            val next = progress + gameSpeed(score)
            if (hitsPlayer(hazard, lane, progress, next)) {
                running = false; over = true; latestBest(score)
            } else if (next > 1.06f) {
                score++; latestBest(score)
                hazard = Random.nextInt(3); progress = -0.15f
            } else progress = next
        }
    }
    fun move(delta: Int) { if (running) lane = (lane + delta).coerceIn(0, 2) }
    Column(Modifier.fillMaxSize().padding(24.dp).onKeyEvent {
        if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionLeft || it.key == Key.DirectionRight)) {
            move(if (it.key == Key.DirectionLeft) -1 else 1); true
        } else false
    }.focusable().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("THE LAYOVER CLUB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Carry-on Dash", style = MaterialTheme.typography.headlineLarge)
        Text("Dodge the lost luggage. Move between three lanes; every bag you pass makes the journey faster. Fully offline.")
        Text("Score $score   ·   Best ${maxOf(best, score)}   ·   Level ${1 + score / 5}", style = MaterialTheme.typography.titleMedium)
        val status = when { over -> "Trip over. Start a new run."; running -> "Running"; else -> "Paused. Press start or resume." }
        Text("$status Your lane: ${lane + 1}. Bag lane: ${hazard + 1}.")
        Canvas(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF173F39), MaterialTheme.shapes.large)
            .semantics { contentDescription = "Three lane game. Your suitcase is in lane ${lane + 1}; obstacle in lane ${hazard + 1}." }) {
            val third = size.width / 3
            for (i in 1..2) drawLine(Color(0xFF52746A), Offset(third * i, 0f), Offset(third * i, size.height), 3f)
            drawCircle(Color(0xFFDEAD71), third * 0.17f, Offset(third * (hazard + 0.5f), size.height * progress))
            drawRoundRect(Color(0xFFBDE7CE), Offset(third * (lane + 0.25f), size.height * 0.76f),
                Size(third * 0.5f, size.height * 0.1f), androidx.compose.ui.geometry.CornerRadius(10f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { move(-1) }, enabled = running && lane > 0, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("← Left") }
            OutlinedButton(onClick = { move(1) }, enabled = running && lane < 2, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Right →") }
        }
        Button(onClick = {
            if (running) running = false else {
                if (over) { score = 0; lane = 1; hazard = Random.nextInt(3); progress = -0.15f; over = false }
                running = true
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Pause" else if (over) "Try again" else if (score == 0 && progress < 0) "Start run" else "Resume") }
        Text("Use the buttons or keyboard arrow keys. Pauses when you leave. Your best score is saved on this device.", style = MaterialTheme.typography.bodySmall)
    }
}

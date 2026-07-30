package social.vyb.app.features.scribble

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText

private val drawColors = listOf(
    "#111827", "#EF4444", "#F97316", "#EAB308", "#22C55E",
    "#06B6D4", "#6366F1", "#EC4899",
)

@Composable
fun ScribbleScreen(
    modifier: Modifier = Modifier,
) {
    val scribbleViewModel: ScribbleViewModel = viewModel()
    val state by scribbleViewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    Box(modifier.fillMaxSize()) {
        if (snapshot == null) {
            ScribbleLobby(
                state = state,
                onRefresh = scribbleViewModel::refreshRooms,
                onCreate = scribbleViewModel::createRoom,
                onJoin = scribbleViewModel::joinRoom,
            )
        } else {
            ScribbleRoom(
                state = state,
                snapshot = snapshot,
                onLeave = scribbleViewModel::leaveRoom,
                onRetry = scribbleViewModel::retryConnection,
                onStart = scribbleViewModel::startGame,
                onChooseWord = scribbleViewModel::chooseWord,
                onDraw = scribbleViewModel::sendDrawStep,
                onClear = scribbleViewModel::clearCanvas,
                onSkip = scribbleViewModel::skipRound,
                onGuess = scribbleViewModel::submitGuess,
            )
        }
        if (state.connection == "connecting" || state.connection == "reconnecting") {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun ScribbleLobby(
    state: ScribbleUiState,
    onRefresh: () -> Unit,
    onCreate: (ScribbleSettings) -> Unit,
    onJoin: (String) -> Unit,
) {
    var roomCode by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Scribble", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Draw, guess and play live with your campus.", color = VybMuted)
                }
                IconButton(onClick = onRefresh, enabled = !state.catalogLoading) {
                    if (state.catalogLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "Refresh rooms")
                }
            }
        }
        state.error?.let { error ->
            item { ConnectionMessage(error, true) }
        }
        state.notice?.let { notice ->
            item { ConnectionMessage(notice, false) }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VybPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Join a room", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = normalizeScribbleRoomCode(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Room code") },
                    )
                    Button(
                        onClick = { onJoin(roomCode) },
                        enabled = state.connection !in setOf("connecting", "reconnecting") && roomCode.length >= 4,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Join room") }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { onCreate(ScribbleSettings()) },
                enabled = state.connection !in setOf("connecting", "reconnecting"),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Brush, contentDescription = null)
                Text("Create private room")
            }
        }
        item {
            Text("Public rooms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (!state.catalogLoading && state.rooms.isEmpty()) {
            item { Text("No public rooms are available right now.", color = VybMuted) }
        }
        items(state.rooms, key = ScribbleCatalogRoom::roomId) { room ->
            Card(
                onClick = { onJoin(room.roomId) },
                colors = CardDefaults.cardColors(containerColor = VybPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = VybTeal)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(room.displayName.ifBlank { room.roomId }, fontWeight = FontWeight.Bold)
                        Text(
                            "${room.playerCount}/${room.maxPlayers} players · ${room.status.lowercase()}",
                            color = VybMuted,
                        )
                    }
                    Text("Join", color = VybTeal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScribbleRoom(
    state: ScribbleUiState,
    snapshot: ScribbleSnapshot,
    onLeave: () -> Unit,
    onRetry: () -> Unit,
    onStart: () -> Unit,
    onChooseWord: (String) -> Unit,
    onDraw: (ScribbleDrawStep) -> Unit,
    onClear: () -> Unit,
    onSkip: () -> Unit,
    onGuess: (String) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(snapshot.timerEndsAt) {
        while (snapshot.timerEndsAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val seconds = remainingSeconds(snapshot.timerEndsAt, now)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Room ${snapshot.displayName.ifBlank { snapshot.roomId }}", fontWeight = FontWeight.Bold)
                Text("${snapshot.status.lowercase()} · ${state.connection}", color = VybMuted)
            }
            if (state.connection == "offline") {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                }
            }
            IconButton(onClick = onLeave) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave room")
            }
        }
        state.error?.let { ConnectionMessage(it, true) }
        state.notice?.let { ConnectionMessage(it, false) }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snapshot.players, key = ScribblePlayer::membershipId) { player ->
                    PlayerChip(player, player.membershipId == snapshot.viewerMembershipId)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Round ${snapshot.round} · Turn ${snapshot.turn}/${snapshot.totalTurns}",
                        color = VybMuted,
                    )
                    Text(snapshot.visibleWord(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                if (snapshot.timerEndsAt != null) {
                    Text("${seconds}s", style = MaterialTheme.typography.headlineSmall, color = VybTeal)
                }
            }

            if (snapshot.status == "LOBBY") {
                Text(
                    if (snapshot.players.count(ScribblePlayer::connected) < 2) {
                        "Share room code ${snapshot.roomId}. At least two players are required."
                    } else {
                        "Players are ready."
                    },
                    color = VybMuted,
                )
                if (snapshot.viewerIsHost && !snapshot.isSystemPublic) {
                    Button(
                        onClick = onStart,
                        enabled = snapshot.players.count(ScribblePlayer::connected) >= 2,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start game") }
                }
            }

            if (snapshot.status == "CHOOSING" && snapshot.wordChoices.isNotEmpty()) {
                Text("Choose a word", fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.wordChoices.forEach { choice ->
                        FilterChip(
                            selected = false,
                            onClick = { onChooseWord(choice.id) },
                            label = { Text("${choice.word} · ${choice.difficulty}") },
                        )
                    }
                }
            }

            ScribbleCanvas(
                snapshot = snapshot,
                onDraw = onDraw,
                onClear = onClear,
                onSkip = onSkip,
            )

            if (
                snapshot.status == "PLAYING" &&
                !snapshot.viewerCanDraw &&
                !snapshot.viewerCorrectThisTurn
            ) {
                GuessComposer(onGuess)
            }

            if (snapshot.status == "ROUND_END") {
                Text(
                    "The word was ${snapshot.roundResult?.word ?: snapshot.revealedWord ?: "hidden"}.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                snapshot.roundResult?.scores?.forEach { score ->
                    Text("${score.displayName}: +${score.delta} · ${score.totalScore}")
                }
            }
            if (snapshot.status == "FINISHED") {
                Text("Final scores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                snapshot.players.sortedByDescending(ScribblePlayer::score).forEachIndexed { index, player ->
                    Text("${index + 1}. ${player.displayName} · ${player.score}")
                }
            }

            Text("Room activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            snapshot.chat.takeLast(20).forEach { chat ->
                Text(
                    if (chat.kind == "system") chat.body else "${chat.displayName}: ${chat.body}",
                    color = if (chat.kind == "correct") VybTeal else VybText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScribbleCanvas(
    snapshot: ScribbleSnapshot,
    onDraw: (ScribbleDrawStep) -> Unit,
    onClear: () -> Unit,
    onSkip: () -> Unit,
) {
    var selectedColor by remember { mutableStateOf(drawColors.first()) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var previous by remember(snapshot.roomId, snapshot.round, snapshot.turn) {
        mutableStateOf<Offset?>(null)
    }
    val canDraw = snapshot.viewerCanDraw

    Box(
        Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, VybBorder, RoundedCornerShape(16.dp))
            .pointerInput(canDraw, selectedColor, strokeWidth) {
                if (!canDraw) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset -> previous = offset },
                    onDragEnd = { previous = null },
                    onDragCancel = { previous = null },
                    onDrag = { change, _ ->
                        val from = previous ?: change.position
                        val to = change.position
                        previous = to
                        onDraw(
                            ScribbleDrawStep(
                                x1 = (from.x / size.width).coerceIn(0f, 1f),
                                y1 = (from.y / size.height).coerceIn(0f, 1f),
                                x2 = (to.x / size.width).coerceIn(0f, 1f),
                                y2 = (to.y / size.height).coerceIn(0f, 1f),
                                color = selectedColor,
                                width = strokeWidth,
                            )
                        )
                        change.consume()
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            snapshot.drawing.forEach { drawScribbleStep(it) }
        }
        if (!canDraw && snapshot.status == "PLAYING" && snapshot.drawing.isEmpty()) {
            Text("Waiting for the drawing…", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
        }
    }

    if (canDraw) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            drawColors.forEach { color ->
                val parsed = remember(color) { Color(color.toColorInt()) }
                Box(
                    Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Drawing color $color"
                            stateDescription =
                                if (selectedColor == color) "Selected" else "Not selected"
                            role = Role.Button
                        }
                        .clickable { selectedColor = color },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(parsed, CircleShape)
                            .border(
                                if (selectedColor == color) 3.dp else 1.dp,
                                VybBorder,
                                CircleShape
                            )
                    )
                }
            }
        }
        Slider(
            value = strokeWidth,
            onValueChange = { strokeWidth = it },
            valueRange = 2f..20f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClear) { Text("Clear") }
            OutlinedButton(onClick = onSkip) { Text("Skip round") }
        }
    }
}

private fun DrawScope.drawScribbleStep(step: ScribbleDrawStep) {
    val color = runCatching { Color(step.color.toColorInt()) }.getOrDefault(Color.Black)
    drawLine(
        color = color,
        start = Offset(step.x1 * size.width, step.y1 * size.height),
        end = Offset(step.x2 * size.width, step.y2 * size.height),
        strokeWidth = step.width,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun GuessComposer(onGuess: (String) -> Unit) {
    var guess by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = guess,
        onValueChange = { guess = it.take(120) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Your guess") },
        keyboardActions = KeyboardActions(
            onDone = {
                if (guess.isNotBlank()) {
                    onGuess(guess)
                    guess = ""
                    focus.clearFocus()
                }
            }
        ),
    )
    Button(
        onClick = {
            onGuess(guess)
            guess = ""
        },
        enabled = guess.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Send guess") }
}

@Composable
private fun PlayerChip(player: ScribblePlayer, isViewer: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isViewer) VybIndigo.copy(alpha = .28f) else VybPanel
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "${player.displayName}${if (player.isHost) " · Host" else ""}",
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${player.score} pts${if (!player.connected) " · offline" else ""}",
                color = VybMuted,
            )
        }
    }
}

@Composable
private fun ConnectionMessage(message: String, error: Boolean) {
    Text(
        message,
        color = if (error) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
            .background(
                if (error) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
            )
            .padding(10.dp),
    )
}

private fun remainingSeconds(value: String?, now: Long): Long {
    val end = value?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: return 0
    return ((end - now + 999) / 1_000).coerceAtLeast(0)
}

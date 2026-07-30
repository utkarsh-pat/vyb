package social.vyb.app.features.funhub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.features.scribble.ScribbleScreen

/**
 * Integration composable for the app nav graph. It only needs a signed-in Firebase user
 * and the API base URL already configured by the mobile module.
 */
@Composable
fun FunHubScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    viewModel: FunViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    LaunchedEffect(viewModel) { viewModel.initialize() }

    VybResponsiveFrame(modifier.fillMaxSize(), maxContentWidth = 900.dp) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Campus Games", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            IconButton(enabled = !state.isRefreshing, onClick = viewModel::refresh) {
                if (state.isRefreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, "Refresh")
            }
        }

        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
            listOf("Connect", "Queens", "Scribble").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }

        when {
            tab == 2 -> ScribbleScreen(Modifier.fillMaxSize())
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VybLoadingMark(width = 104.dp)
            }
            state.error != null && state.connect == null && state.queens == null ->
                FullError(state.error!!, viewModel::refresh)
            else -> Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> ConnectPane(state, viewModel)
                    else -> QueensPane(state, viewModel)
                }
                state.error?.let { StatusBanner(it, true, Modifier.align(Alignment.BottomCenter)) }
                state.message?.let { StatusBanner(it, false, Modifier.align(Alignment.BottomCenter)) }
            }
        }
    }
    }
}

@Composable
private fun ConnectPane(state: FunUiState, viewModel: FunViewModel) {
    val game = state.connect
    if (game == null) return FullError("Connect challenge is unavailable.", viewModel::refresh)
    val level = game.level
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeading("Daily Connect #${game.dailyIndex}", level.difficulty, "Tap dots in route order.")
        Grid(level.gridSize) { x, y ->
            val dot = level.dots.firstOrNull { it.x == x && it.y == y }
            val coordinate = Coordinate(x, y)
            val index = state.connectPath.indexOf(coordinate)
            Box(
                Modifier.size(48.dp)
                    .then(
                        if (dot != null) Modifier.background(
                            if (index >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                            .semantics {
                                contentDescription =
                                    "Connect dot ${dot.id}, row ${y + 1}, column ${x + 1}"
                                stateDescription =
                                    if (index >= 0) "Selected ${index + 1}" else "Not selected"
                                role = Role.Button
                            }
                            .clickable { viewModel.chooseConnect(dot) }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (dot != null) Text(
                    if (index >= 0) "${index + 1}" else "${dot.id}",
                    color = if (index >= 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text("${state.connectPath.size}/${level.dots.size} dots selected", Modifier.padding(top = 14.dp))
        GameActions(
            busy = state.isActionRunning,
            canSubmit = state.connectPath.size == level.dots.size,
            onHint = viewModel::hintConnect,
            onSubmit = viewModel::submitConnect
        )
    }
}

@Composable
private fun QueensPane(state: FunUiState, viewModel: FunViewModel) {
    val game = state.queens
    if (game == null) return FullError("Queens challenge is unavailable.", viewModel::refresh)
    val level = game.level
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeading("Daily Queens #${game.dailyIndex}", level.difficulty, "Place one queen in every row, column and region.")
        Grid(level.gridSize) { x, y ->
            val point = Coordinate(x, y)
            val region = level.regions.getOrNull(y)?.getOrNull(x) ?: 0
            val selected = point in state.queenCells
            Box(
                Modifier.size(48.dp)
                    .background(regionColor(region))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f))
                    .semantics {
                        contentDescription =
                            "Queens cell row ${y + 1}, column ${x + 1}, region ${region + 1}"
                        stateDescription = if (selected) "Queen placed" else "Empty"
                        role = Role.Button
                    }
                    .clickable { viewModel.toggleQueen(point) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text("♛", style = MaterialTheme.typography.headlineSmall)
                else if (point in state.markedCells) Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("${state.queenCells.size}/${level.gridSize} queens placed", Modifier.padding(top = 14.dp))
        GameActions(
            busy = state.isActionRunning,
            canSubmit = state.queenCells.size == level.gridSize,
            onHint = viewModel::hintQueens,
            onSubmit = viewModel::submitQueens
        )
    }
}

@Composable
private fun Grid(size: Int, cell: @Composable (x: Int, y: Int) -> Unit) {
    Column(
        Modifier
            .padding(top = 18.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        repeat(size) { y ->
            Row { repeat(size) { x -> cell(x, y) } }
        }
    }
}

@Composable
private fun GameHeading(title: String, difficulty: String, instructions: String) {
    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    Text(difficulty, color = MaterialTheme.colorScheme.primary)
    Text(instructions, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
}

@Composable
private fun GameActions(busy: Boolean, canSubmit: Boolean, onHint: () -> Unit, onSubmit: () -> Unit) {
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(enabled = !busy, onClick = onHint) { Text("Hint") }
        Button(enabled = !busy && canSubmit, onClick = onSubmit) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Submit")
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun FullError(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = retry, modifier = Modifier.padding(top = 12.dp)) { Text("Try again") }
        }
    }
}

@Composable
private fun StatusBanner(message: String, error: Boolean, modifier: Modifier = Modifier) {
    Text(
        message,
        color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
            .background(
                if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            )
            .padding(12.dp)
    )
}

private fun regionColor(region: Int): Color {
    val palette = listOf(
        Color(0xFFE8DEF8), Color(0xFFFFDDB3), Color(0xFFC4EED0), Color(0xFFFFDAD6),
        Color(0xFFC2E7FF), Color(0xFFF2D8FF), Color(0xFFE1E3A6), Color(0xFFFFD8E4)
    )
    return palette[Math.floorMod(region, palette.size)]
}

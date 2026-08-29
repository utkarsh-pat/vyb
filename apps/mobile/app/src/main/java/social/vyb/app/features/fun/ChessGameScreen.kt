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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText

private enum class ChessColor { WHITE, BLACK }
private enum class ChessKind(val asset: String, val notation: String) {
    KING("k", "K"), QUEEN("q", "Q"), ROOK("r", "R"), BISHOP("b", "B"), KNIGHT("n", "N"), PAWN("p", "")
}

private data class ChessPiece(val color: ChessColor, val kind: ChessKind)
private data class ChessMove(val from: Int, val to: Int, val castle: Boolean = false, val enPassant: Boolean = false)
private data class ChessPosition(
    val board: List<ChessPiece?>,
    val turn: ChessColor = ChessColor.WHITE,
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true,
    val enPassantSquare: Int? = null,
    val notation: String = "",
)

private val chessLight = Color(0xFFEEEED2)
private val chessDark = Color(0xFF769656)
private val chessSelected = Color(0xFFF2ED58)
private val chessLastMove = Color(0xFFDDE54C).copy(alpha = .72f)

@Composable
fun ChessGameScreen(modifier: Modifier = Modifier) {
    var onlineMode by remember { mutableStateOf(false) }
    if (onlineMode) {
        Box(modifier.fillMaxSize()) {
            AuthenticatedWebGameScreen("chess", Modifier.fillMaxSize())
            Text(
                "Local board",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xDD111F35))
                    .clickable { onlineMode = false }.padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        return
    }
    val timeline = remember { mutableStateListOf(initialChessPosition()) }
    var viewIndex by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var flipped by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    val liveIndex = timeline.lastIndex
    val position = timeline[viewIndex]
    val isLive = viewIndex == liveIndex
    val legalTargets = remember(position, selected, isLive) {
        if (!isLive || selected == null) emptyList() else legalMoves(position).filter { it.from == selected }.map { it.to }
    }

    LaunchedEffect(viewIndex) { selected = null }

    Column(modifier.fillMaxSize().background(Color(0xFF061326))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                MoveHistoryRail(timeline = timeline, selectedPly = viewIndex, onSelect = { viewIndex = it })
            }
            Text(
                "Online",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(10.dp))
                    .background(VybIndigo).clickable { onlineMode = true }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
        PlayerStrip(
            name = "Player 2",
            color = ChessColor.BLACK,
            active = position.turn == ChessColor.BLACK && isLive,
            inCheck = isKingInCheck(position, ChessColor.BLACK),
        )
        ChessBoard(
            position = position,
            selected = selected,
            legalTargets = legalTargets,
            flipped = flipped,
            onSquare = { square ->
                if (!isLive) return@ChessBoard
                val chosen = selected
                val move = if (chosen == null) null else legalMoves(position).firstOrNull { it.from == chosen && it.to == square }
                when {
                    move != null -> {
                        val next = applyLegalMove(position, move)
                        timeline += next
                        viewIndex = timeline.lastIndex
                        selected = null
                    }
                    position.board[square]?.color == position.turn -> selected = square
                    else -> selected = null
                }
            },
        )
        PlayerStrip(
            name = "Player 1",
            color = ChessColor.WHITE,
            active = position.turn == ChessColor.WHITE && isLive,
            inCheck = isKingInCheck(position, ChessColor.WHITE),
        )
        ChessStatus(position = position, viewIndex = viewIndex, liveIndex = liveIndex)
        Spacer(Modifier.weight(1f))
        Box {
            ChessActionDock(
                isLive = isLive,
                canBack = viewIndex > 0,
                canForward = viewIndex < liveIndex,
                onOptions = { optionsOpen = true },
                onLive = { viewIndex = liveIndex },
                onBack = { if (viewIndex > 0) viewIndex-- },
                onForward = { if (viewIndex < liveIndex) viewIndex++ },
            )
            DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Flip board") },
                    leadingIcon = { Icon(Icons.Outlined.Cached, null) },
                    onClick = { flipped = !flipped; selected = null; optionsOpen = false },
                )
                DropdownMenuItem(
                    text = { Text("New game") },
                    onClick = {
                        timeline.clear()
                        timeline += initialChessPosition()
                        viewIndex = 0
                        selected = null
                        optionsOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MoveHistoryRail(timeline: List<ChessPosition>, selectedPly: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).horizontalScroll(rememberScrollState())
            .background(Color(0xFF050D1A)).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (timeline.size == 1) Text("No moves yet", color = VybMuted, fontSize = 12.sp)
        timeline.drop(1).forEachIndexed { index, state ->
            val ply = index + 1
            Text(
                text = if (index % 2 == 0) "${index / 2 + 1}. ${state.notation}" else state.notation,
                color = if (selectedPly == ply) Color.White else VybMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (selectedPly == ply) Color(0xFF3B4865) else Color.Transparent)
                    .clickable { onSelect(ply) }.padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PlayerStrip(name: String, color: ChessColor, active: Boolean, inCheck: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF111F35))
            .border(1.dp, if (active) VybTeal.copy(alpha = .7f) else VybBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF223A5E)),
            contentAlignment = Alignment.Center,
        ) { Text(name.take(1), color = Color.White, fontWeight = FontWeight.Black) }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(name, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (color == ChessColor.WHITE) "White" else "Black"}${if (active) " to move" else " waiting"}${if (inCheck) " · Check" else ""}",
                color = Color(0xFFAEBAD0),
                fontSize = 11.sp,
            )
        }
        Text(if (active) "TURN" else "ONLINE", color = VybTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChessBoard(
    position: ChessPosition,
    selected: Int?,
    legalTargets: List<Int>,
    flipped: Boolean,
    onSquare: (Int) -> Unit,
) {
    val rowOrder = if (flipped) (7 downTo 0) else (0..7)
    val columnOrder = if (flipped) (7 downTo 0) else (0..7)
    Column(Modifier.fillMaxWidth().aspectRatio(1f).border(3.dp, Color(0xFF31422F))) {
        rowOrder.forEach { row ->
            Row(Modifier.weight(1f)) {
                columnOrder.forEach { column ->
                    val square = row * 8 + column
                    val piece = position.board[square]
                    val isLight = (row + column) % 2 == 0
                    val lastMove = position.notation.takeIf { it.isNotBlank() }
                    val background = when {
                        selected == square -> chessSelected
                        lastMove != null && square in lastMoveSquares(position) -> chessLastMove
                        isLight -> chessLight
                        else -> chessDark
                    }
                    Box(
                        Modifier.weight(1f).fillMaxSize().background(background)
                            .semantics { contentDescription = squareName(square); role = Role.Button }
                            .clickable { onSquare(square) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (piece != null) {
                            AsyncImage(
                                model = "file:///android_asset/chess/${if (piece.color == ChessColor.WHITE) "w" else "b"}${piece.kind.asset}.svg",
                                contentDescription = "${piece.color.name.lowercase()} ${piece.kind.name.lowercase()}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(.88f),
                            )
                        }
                        if (square in legalTargets) {
                            Box(
                                Modifier.size(if (piece == null) 13.dp else 34.dp).clip(CircleShape)
                                    .then(
                                        if (piece == null) Modifier.background(Color(0x66224A2C))
                                        else Modifier.border(4.dp, Color(0x99512626), CircleShape)
                                    ),
                            )
                        }
                        val showRank = column == columnOrder.first
                        val showFile = row == rowOrder.last
                        if (showRank || showFile) {
                            Text(
                                text = "${if (showRank) 8 - row else ""}${if (showFile) ('a'.code + column).toChar() else ""}",
                                color = if (isLight) chessDark else chessLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessStatus(position: ChessPosition, viewIndex: Int, liveIndex: Int) {
    val legal = remember(position) { legalMoves(position) }
    val status = when {
        legal.isEmpty() && isKingInCheck(position, position.turn) -> "Checkmate"
        legal.isEmpty() -> "Draw"
        viewIndex < liveIndex -> "History"
        else -> "Live position"
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(
            if (viewIndex < liveIndex) "Viewing move $viewIndex of $liveIndex" else position.notation.ifBlank { "Opening position" },
            color = VybMuted,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
        Text(status, color = VybTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChessActionDock(
    isLive: Boolean,
    canBack: Boolean,
    canForward: Boolean,
    onOptions: () -> Unit,
    onLive: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(70.dp).background(Color(0xFF050D1A)).padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockAction("Options", Icons.Outlined.Menu, true, onOptions)
        DockAction("Chat", Icons.Outlined.ChatBubbleOutline, false, {})
        DockAction("Live", Icons.Outlined.GpsFixed, !isLive, onLive, highlighted = true)
        DockAction("Back", Icons.AutoMirrored.Filled.KeyboardArrowLeft, canBack, onBack)
        DockAction("Forward", Icons.AutoMirrored.Filled.KeyboardArrowRight, canForward, onForward)
    }
}

@Composable
private fun DockAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Column(
        Modifier.width(66.dp).clip(RoundedCornerShape(13.dp))
            .background(if (highlighted) VybIndigo.copy(alpha = .9f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (enabled || highlighted) Color.White else VybMuted.copy(alpha = .38f), modifier = Modifier.size(23.dp))
        Text(label, color = if (enabled || highlighted) Color.White else VybMuted.copy(alpha = .38f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun initialChessPosition(): ChessPosition {
    val board = MutableList<ChessPiece?>(64) { null }
    val order = listOf(ChessKind.ROOK, ChessKind.KNIGHT, ChessKind.BISHOP, ChessKind.QUEEN, ChessKind.KING, ChessKind.BISHOP, ChessKind.KNIGHT, ChessKind.ROOK)
    order.forEachIndexed { column, kind ->
        board[column] = ChessPiece(ChessColor.BLACK, kind)
        board[8 + column] = ChessPiece(ChessColor.BLACK, ChessKind.PAWN)
        board[48 + column] = ChessPiece(ChessColor.WHITE, ChessKind.PAWN)
        board[56 + column] = ChessPiece(ChessColor.WHITE, kind)
    }
    return ChessPosition(board)
}

private fun legalMoves(position: ChessPosition): List<ChessMove> = buildList {
    position.board.forEachIndexed { from, piece ->
        if (piece?.color != position.turn) return@forEachIndexed
        pseudoMoves(position, from, piece).forEach { move ->
            val next = applyMove(position, move, notation = false)
            if (!isKingInCheck(next, piece.color)) add(move)
        }
    }
}

private fun pseudoMoves(position: ChessPosition, from: Int, piece: ChessPiece): List<ChessMove> {
    val row = from / 8
    val col = from % 8
    val moves = mutableListOf<ChessMove>()
    fun add(rowTarget: Int, colTarget: Int): Boolean {
        if (rowTarget !in 0..7 || colTarget !in 0..7) return false
        val to = rowTarget * 8 + colTarget
        val target = position.board[to]
        if (target?.color == piece.color) return false
        moves += ChessMove(from, to)
        return target == null
    }
    when (piece.kind) {
        ChessKind.PAWN -> {
            val step = if (piece.color == ChessColor.WHITE) -1 else 1
            val start = if (piece.color == ChessColor.WHITE) 6 else 1
            val oneRow = row + step
            if (oneRow in 0..7 && position.board[oneRow * 8 + col] == null) {
                moves += ChessMove(from, oneRow * 8 + col)
                val twoRow = row + step * 2
                if (row == start && position.board[twoRow * 8 + col] == null) moves += ChessMove(from, twoRow * 8 + col)
            }
            listOf(col - 1, col + 1).forEach { captureCol ->
                if (oneRow in 0..7 && captureCol in 0..7) {
                    val to = oneRow * 8 + captureCol
                    if (position.board[to]?.color == piece.color.opposite()) moves += ChessMove(from, to)
                    else if (position.enPassantSquare == to) moves += ChessMove(from, to, enPassant = true)
                }
            }
        }
        ChessKind.KNIGHT -> listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1).forEach { (dr, dc) -> add(row + dr, col + dc) }
        ChessKind.BISHOP -> addSliding(moves, position, from, piece.color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
        ChessKind.ROOK -> addSliding(moves, position, from, piece.color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
        ChessKind.QUEEN -> addSliding(moves, position, from, piece.color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1))
        ChessKind.KING -> {
            for (dr in -1..1) for (dc in -1..1) if (dr != 0 || dc != 0) add(row + dr, col + dc)
            val kingSide = if (piece.color == ChessColor.WHITE) position.whiteKingSide else position.blackKingSide
            val queenSide = if (piece.color == ChessColor.WHITE) position.whiteQueenSide else position.blackQueenSide
            if (!isKingInCheck(position, piece.color)) {
                if (kingSide && position.board[row * 8 + 5] == null && position.board[row * 8 + 6] == null &&
                    !isSquareAttacked(position, row * 8 + 5, piece.color.opposite()) && !isSquareAttacked(position, row * 8 + 6, piece.color.opposite())) {
                    moves += ChessMove(from, row * 8 + 6, castle = true)
                }
                if (queenSide && position.board[row * 8 + 1] == null && position.board[row * 8 + 2] == null && position.board[row * 8 + 3] == null &&
                    !isSquareAttacked(position, row * 8 + 3, piece.color.opposite()) && !isSquareAttacked(position, row * 8 + 2, piece.color.opposite())) {
                    moves += ChessMove(from, row * 8 + 2, castle = true)
                }
            }
        }
    }
    return moves
}

private fun addSliding(moves: MutableList<ChessMove>, position: ChessPosition, from: Int, color: ChessColor, directions: List<Pair<Int, Int>>) {
    directions.forEach { (dr, dc) ->
        var row = from / 8 + dr
        var col = from % 8 + dc
        while (row in 0..7 && col in 0..7) {
            val to = row * 8 + col
            val target = position.board[to]
            if (target == null) moves += ChessMove(from, to)
            else {
                if (target.color != color) moves += ChessMove(from, to)
                break
            }
            row += dr
            col += dc
        }
    }
}

private fun applyLegalMove(position: ChessPosition, move: ChessMove): ChessPosition {
    val moved = applyMove(position, move, notation = false)
    val check = isKingInCheck(moved, moved.turn)
    val mate = check && legalMoves(moved).isEmpty()
    val piece = position.board[move.from] ?: return position
    val capture = position.board[move.to] != null || move.enPassant
    val base = when {
        move.castle && move.to % 8 == 6 -> "O-O"
        move.castle -> "O-O-O"
        else -> "${piece.kind.notation}${if (capture) "${if (piece.kind == ChessKind.PAWN) squareName(move.from).first() else ""}x" else ""}${squareName(move.to)}"
    }
    return moved.copy(notation = base + if (mate) "#" else if (check) "+" else "")
}

private fun applyMove(position: ChessPosition, move: ChessMove, notation: Boolean): ChessPosition {
    val board = position.board.toMutableList()
    val piece = board[move.from] ?: return position
    val captured = board[move.to]
    board[move.from] = null
    if (move.enPassant) board[move.to + if (piece.color == ChessColor.WHITE) 8 else -8] = null
    val targetRow = move.to / 8
    board[move.to] = if (piece.kind == ChessKind.PAWN && targetRow in listOf(0, 7)) piece.copy(kind = ChessKind.QUEEN) else piece
    if (move.castle) {
        val row = move.from / 8
        if (move.to % 8 == 6) {
            board[row * 8 + 5] = board[row * 8 + 7]
            board[row * 8 + 7] = null
        } else {
            board[row * 8 + 3] = board[row * 8]
            board[row * 8] = null
        }
    }
    var wks = position.whiteKingSide
    var wqs = position.whiteQueenSide
    var bks = position.blackKingSide
    var bqs = position.blackQueenSide
    if (piece.kind == ChessKind.KING) if (piece.color == ChessColor.WHITE) { wks = false; wqs = false } else { bks = false; bqs = false }
    if (move.from == 56 || move.to == 56) wqs = false
    if (move.from == 63 || move.to == 63) wks = false
    if (move.from == 0 || move.to == 0) bqs = false
    if (move.from == 7 || move.to == 7) bks = false
    val enPassant = if (piece.kind == ChessKind.PAWN && kotlin.math.abs(move.to - move.from) == 16) (move.from + move.to) / 2 else null
    return ChessPosition(board, piece.color.opposite(), wks, wqs, bks, bqs, enPassant, if (notation) position.notation else "")
}

private fun isKingInCheck(position: ChessPosition, color: ChessColor): Boolean {
    val king = position.board.indexOfFirst { it?.color == color && it.kind == ChessKind.KING }
    return king >= 0 && isSquareAttacked(position, king, color.opposite())
}

private fun isSquareAttacked(position: ChessPosition, square: Int, by: ChessColor): Boolean {
    val row = square / 8
    val col = square % 8
    val pawnRow = row + if (by == ChessColor.WHITE) 1 else -1
    for (pawnCol in listOf(col - 1, col + 1)) if (pawnRow in 0..7 && pawnCol in 0..7 && position.board[pawnRow * 8 + pawnCol] == ChessPiece(by, ChessKind.PAWN)) return true
    for ((dr, dc) in listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)) {
        val r = row + dr; val c = col + dc
        if (r in 0..7 && c in 0..7 && position.board[r * 8 + c] == ChessPiece(by, ChessKind.KNIGHT)) return true
    }
    fun ray(directions: List<Pair<Int, Int>>, kinds: Set<ChessKind>): Boolean {
        for ((dr, dc) in directions) {
            var r = row + dr; var c = col + dc
            while (r in 0..7 && c in 0..7) {
                val piece = position.board[r * 8 + c]
                if (piece != null) { if (piece.color == by && piece.kind in kinds) return true; break }
                r += dr; c += dc
            }
        }
        return false
    }
    if (ray(listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), setOf(ChessKind.ROOK, ChessKind.QUEEN))) return true
    if (ray(listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), setOf(ChessKind.BISHOP, ChessKind.QUEEN))) return true
    for (dr in -1..1) for (dc in -1..1) if (dr != 0 || dc != 0) {
        val r = row + dr; val c = col + dc
        if (r in 0..7 && c in 0..7 && position.board[r * 8 + c] == ChessPiece(by, ChessKind.KING)) return true
    }
    return false
}

private fun ChessColor.opposite() = if (this == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
private fun squareName(square: Int) = "${('a'.code + square % 8).toChar()}${8 - square / 8}"
private fun lastMoveSquares(position: ChessPosition): Set<Int> {
    if (position.notation.isBlank()) return emptySet()
    val destination = Regex("[a-h][1-8]").findAll(position.notation).lastOrNull()?.value ?: return emptySet()
    val column = destination[0] - 'a'
    val row = 8 - destination[1].digitToInt()
    return setOf(row * 8 + column)
}

internal object NativeChessRulesTestApi {
    fun openingLegalMoveCount(): Int = legalMoves(initialChessPosition()).size

    fun play(vararg coordinateMoves: String): List<String>? {
        var position = initialChessPosition()
        val history = mutableListOf<String>()
        coordinateMoves.forEach { coordinate ->
            if (coordinate.length != 4) return null
            val from = squareIndex(coordinate.substring(0, 2)) ?: return null
            val to = squareIndex(coordinate.substring(2, 4)) ?: return null
            val move = legalMoves(position).firstOrNull { it.from == from && it.to == to } ?: return null
            position = applyLegalMove(position, move)
            history += position.notation
        }
        return history
    }

    private fun squareIndex(value: String): Int? {
        val column = value.getOrNull(0)?.minus('a') ?: return null
        val rank = value.getOrNull(1)?.digitToIntOrNull() ?: return null
        if (column !in 0..7 || rank !in 1..8) return null
        return (8 - rank) * 8 + column
    }
}

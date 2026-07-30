package social.vyb.app.features.funhub

import kotlinx.serialization.Serializable

@Serializable
data class Coordinate(val x: Int, val y: Int)

@Serializable
data class ConnectDot(val id: Int, val x: Int, val y: Int)

@Serializable
data class ConnectLevel(
    val levelId: Int,
    val gridSize: Int,
    val dots: List<ConnectDot> = emptyList(),
    val difficulty: String
)

@Serializable
data class ConnectDaily(
    val sessionId: String,
    val dailyIndex: Int,
    val dailyKey: String,
    val hintsUsed: Int = 0,
    val sessionCompletedAt: String? = null,
    val level: ConnectLevel
)

@Serializable
data class ConnectMoveRequest(val sessionId: String, val path: List<Coordinate>)

@Serializable
data class ConnectSubmitRequest(
    val sessionId: String,
    val path: List<Coordinate>,
    val clientElapsedSeconds: Int? = null
)

@Serializable
data class ConnectHint(
    val sessionId: String,
    val message: String,
    val nextMove: Coordinate? = null,
    val hintsUsed: Int = 0
)

@Serializable
data class ConnectSubmitResult(
    val solved: Boolean,
    val message: String,
    val sessionId: String
)

@Serializable
data class QueensLevel(
    val levelId: Int,
    val gridSize: Int,
    val regionCount: Int,
    val regions: List<List<Int>> = emptyList(),
    val difficulty: String
)

@Serializable
data class QueensDaily(
    val sessionId: String,
    val dailyIndex: Int,
    val dailyKey: String,
    val hintsUsed: Int = 0,
    val errorsMade: Int = 0,
    val sessionCompletedAt: String? = null,
    val level: QueensLevel
)

@Serializable
data class QueensHintRequest(
    val sessionId: String,
    val queens: List<Coordinate>,
    val marks: List<Coordinate> = emptyList()
)

@Serializable
data class QueensSubmitRequest(
    val sessionId: String,
    val queens: List<Coordinate>,
    val clientElapsedSeconds: Int? = null
)

@Serializable
data class QueensHint(
    val sessionId: String,
    val message: String,
    val errorCells: List<Coordinate> = emptyList(),
    val autoMarkCells: List<Coordinate> = emptyList(),
    val nextQueen: Coordinate? = null,
    val hintsUsed: Int = 0,
    val errorsMade: Int = 0
)

@Serializable
data class QueensSubmitResult(
    val solved: Boolean,
    val message: String,
    val sessionId: String,
    val errorCells: List<Coordinate> = emptyList()
)

data class FunUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isActionRunning: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val connect: ConnectDaily? = null,
    val connectPath: List<Coordinate> = emptyList(),
    val queens: QueensDaily? = null,
    val queenCells: Set<Coordinate> = emptySet(),
    val markedCells: Set<Coordinate> = emptySet()
)

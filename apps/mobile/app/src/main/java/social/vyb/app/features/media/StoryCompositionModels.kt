package social.vyb.app.features.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class StoryCompositionJson(
    val version: Int = 1,
    val canvas: StoryCanvasJson = StoryCanvasJson(),
    val media: StoryMediaJson,
    val layers: List<StoryLayerJson>
)

@Serializable
internal data class StoryCanvasJson(
    val width: Int = 1080,
    val height: Int = 1920
)

@Serializable
internal data class StoryMediaJson(
    val fit: String,
    val scale: Float,
    val rotationDegrees: Float,
    val offsetX: Float,
    val offsetY: Float
)

@Serializable
internal data class StoryLayerJson(
    val type: String,
    val id: String,
    val x: Float? = null,
    val y: Float? = null,
    val text: String? = null,
    val value: String? = null,
    val color: String? = null,
    val fontSize: Float? = null,
    val align: String? = null,
    val style: String? = null,
    val size: Float? = null,
    val width: Float? = null,
    val points: List<StoryPointJson>? = null
)

@Serializable
internal data class StoryPointJson(val x: Float, val y: Float)

internal val StoryCompositionCodec = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

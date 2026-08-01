package social.vyb.app.features.social
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.data.readBytesAtMost
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialThreadSheet(
    postId: String,
    thread: CommentThreadState,
    onLoad: () -> Unit,
    onRetry: () -> Unit,
    onAddComment: (text: String, parentCommentId: String?, isAnonymous: Boolean, mediaUrl: String?, mediaType: String?, onAdded: () -> Unit) -> Unit,
    onToggleCommentReaction: (commentId: String) -> Unit,
    onUpdateComment: (commentId: String, body: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onReportComment: (commentId: String, reason: String) -> Unit,
    busyCommentIds: Set<String>,
    onDismiss: () -> Unit
) {
    var text by remember(postId) { mutableStateOf("") }
    var replyTo by remember(postId) { mutableStateOf<SocialComment?>(null) }
    var isAnonymous by remember(postId) { mutableStateOf(false) }
    var mediaUrl by remember(postId) { mutableStateOf<String?>(null) }
    var mediaType by remember(postId) { mutableStateOf<String?>(null) }
    var gifTrayOpen by remember(postId) { mutableStateOf(false) }
    var mediaError by remember(postId) { mutableStateOf<String?>(null) }
    var mediaUploading by remember(postId) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaRepository = remember { social.vyb.app.features.media.MediaComposerRepository() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                mediaUploading = true
                runCatching {
                    val mime = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") }
                        ?: error("Choose a supported image.")
                    mediaRepository.uploadCommentImage(
                        resolver = context.contentResolver,
                        uri = uri,
                        fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "comment-image",
                        mimeType = mime
                    )
                }.onSuccess {
                    mediaUrl = it.url
                    mediaType = "image"
                    mediaError = null
                }.onFailure { mediaError = it.message ?: "Could not attach image." }
                mediaUploading = false
            }
        }
    }
    LaunchedEffect(postId) { onLoad() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VybPanel,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VybText)
                Spacer(Modifier.weight(1f))
}
            when {
                thread.loading -> {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
}
}
                thread.error != null && thread.items.isEmpty() -> {
                    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(thread.error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Retry") }
}
}
                thread.items.isEmpty() -> {
                    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("No comments yet. Start the conversation.", color = VybMuted)
}
}
                else -> {
                    val roots = remember(thread.items) { buildCommentThread(thread.items) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(roots.size, key = { roots[it].comment.id }) { index ->
                            val node = roots[index]
                            CommentNodeRow(
                                node = node,
                                busyCommentIds = busyCommentIds,
                                onReply = { replyTo = node.comment },
                                onToggleReaction = onToggleCommentReaction,
                                onUpdate = onUpdateComment,
                                onDelete = onDeleteComment,
                                onReport = onReportComment
                            )
}
}
}
}
            thread.error?.takeIf { thread.items.isNotEmpty() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
}
            Spacer(Modifier.height(8.dp))
            if (gifTrayOpen) {
                val gifs = remember {
                    listOf(
                        "https://media.giphy.com/media/3o7abKhOpu0NwenH3O/giphy.gif",
                        "https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif",
                        "https://media.giphy.com/media/10JhviFuU2gWD6/giphy.gif",
                        "https://media.giphy.com/media/3oz8xIsloV7zOmt81G/giphy.gif"
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    items(gifs) { gif ->
                        VybRemoteImage(
                            url = gif,
                            contentDescription = "Select GIF",
                            modifier = Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)).clickable {
                                mediaUrl = gif
                                mediaType = "gif"
                                gifTrayOpen = false
                            }
                        )
                    }
                }
            }
            // Quick Emojis
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                val emojis = listOf("❤️", "🙌", "🔥", "😂", "😮", "😢", "😍", "👍", "👏", "🤩", "🤯")
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .clickable { text += emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
}
}
}
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                SocialAvatar(
                    avatarUrl = null,
                    displayName = if (isAnonymous) "Anonymous" else "You",
                    size = 40.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (isAnonymous) "Anonymous Vyber" else "You", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = VybText)
}
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isAnonymous = !isAnonymous }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(if (isAnonymous) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(4.dp))
                                    .border(1.dp, if (isAnonymous) Color.Transparent else VybMuted, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAnonymous) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
}
}
                            Spacer(Modifier.width(6.dp))
                            Text("Anonymous", style = MaterialTheme.typography.labelSmall, color = VybText)
}
}
                    replyTo?.let { target ->
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().background(VybPanelLifted, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Replying to @${target.author?.username ?: "member"}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { replyTo = null }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(12.dp), tint = VybText)
}
}
}
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = VybText),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(if (replyTo != null) "Write your reply..." else "Write a comment...", color = VybMuted, style = MaterialTheme.typography.bodyMedium)
}
                            innerTextField()
}
                    )
                    Spacer(Modifier.height(12.dp))
                    mediaUrl?.let { selectedUrl ->
                        Row(
                            Modifier.fillMaxWidth().background(VybPanelLifted, RoundedCornerShape(10.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VybRemoteImage(selectedUrl, "Selected comment media", Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)))
                            Text(mediaType?.uppercase(Locale.US) ?: "MEDIA", Modifier.padding(start = 10.dp).weight(1f), color = VybMuted)
                            IconButton(onClick = { mediaUrl = null; mediaType = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove media")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    mediaError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(VybPanelLifted, RoundedCornerShape(8.dp))
                                    .clickable { gifTrayOpen = !gifTrayOpen }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("GIF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = VybText)
                            }
                            Box(
                                modifier = Modifier
                                    .background(VybPanelLifted, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !mediaUploading) { imagePicker.launch("image/*") }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (mediaUploading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = VybText)
                                } else {
                                    Icon(Icons.Outlined.Image, contentDescription = "Image", modifier = Modifier.size(16.dp), tint = VybText)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                onAddComment(text, replyTo?.id, isAnonymous, mediaUrl, mediaType) {
                                    text = ""
                                    replyTo = null
                                    mediaUrl = null
                                    mediaType = null
}
                            },
                            enabled = (text.trim().isNotEmpty() || mediaUrl != null) &&
                                !thread.submitting && !mediaUploading,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp, minWidth = 60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = VybPanelLifted,
                                disabledContentColor = VybMuted
                            )
                        ) {
                            if (thread.submitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Post", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
data class FlatCommentNode(
    val comment: SocialComment,
    val depth: Int,
    val isLastChild: Boolean,
    val hasChildren: Boolean,
    val activeParentDepths: Set<Int>,
    val replyTargetUsername: String?
)
private fun buildCommentThread(comments: List<SocialComment>): List<FlatCommentNode> {
    val nodesMap = comments.associateBy { it.id }
    val childrenMap = mutableMapOf<String, MutableList<SocialComment>>()
    val roots = mutableListOf<SocialComment>()
    comments.forEach { comment ->
        val parentId = comment.parentCommentId
        if (parentId != null && nodesMap.containsKey(parentId)) {
            childrenMap.getOrPut(parentId) { mutableListOf() }.add(comment)
        } else {
            roots.add(comment)
        }
    }
    val flatList = mutableListOf<FlatCommentNode>()
    roots.forEach { root ->
        fun dfs(node: SocialComment, depth: Int, isLast: Boolean, activeDepths: Set<Int>) {
            val replyTarget = if (depth > 0 && node.parentCommentId != null) {
                nodesMap[node.parentCommentId]?.author?.username
            } else null

            val children = childrenMap[node.id] ?: emptyList()
            flatList.add(
                FlatCommentNode(
                    comment = node,
                    depth = depth,
                    isLastChild = isLast,
                    hasChildren = children.isNotEmpty(),
                    activeParentDepths = activeDepths,
                    replyTargetUsername = replyTarget
                )
            )

            children.forEachIndexed { index, child ->
                val isChildLast = index == children.size - 1
                val childActiveDepths = if (isChildLast) activeDepths else activeDepths + depth
                dfs(child, depth + 1, isChildLast, childActiveDepths)
            }
        }
        dfs(root, 0, true, emptySet())
    }
    return flatList
}
@Composable
private fun CommentNodeRow(
    node: FlatCommentNode,
    busyCommentIds: Set<String>,
    onReply: (SocialComment) -> Unit,
    onToggleReaction: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReport: (String, String) -> Unit
) {
    val comment = node.comment
    val isBusy = comment.id in busyCommentIds
    val paddingStart = (node.depth * 38).dp
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val replyThreshold = with(density) { 60.dp.toPx() }
    val threadLineColor = VybBorder
    var menuOpen by remember(comment.id) { mutableStateOf(false) }
    var editOpen by remember(comment.id) { mutableStateOf(false) }
    var deleteOpen by remember(comment.id) { mutableStateOf(false) }
    var reportOpen by remember(comment.id) { mutableStateOf(false) }
    var editText by remember(comment.id, comment.body) { mutableStateOf(comment.body) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val spacing = 38.dp.toPx()
                val startX = 30.dp.toPx()

                // Draw active parent vertical lines passing all the way through
                node.activeParentDepths.forEach { d ->
                    val lineX = startX + (d * spacing)
                    drawLine(
                        color = threadLineColor,
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height),
                        strokeWidth = strokeWidth
                    )
                }

                // If not root, draw the elbow from the parent
                if (node.depth > 0) {
                    val parentX = startX + ((node.depth - 1) * spacing)
                    val elbowY = 32.dp.toPx()
                    val cornerRadius = 10.dp.toPx()

                    val path = Path().apply {
                        moveTo(parentX, 0f)
                        lineTo(parentX, elbowY - cornerRadius)
                        quadraticTo(
                            parentX, elbowY,
                            parentX + cornerRadius, elbowY
                        )
                        lineTo(parentX + 8.dp.toPx(), elbowY)
                    }
                    val bottomY = if (node.isLastChild) elbowY else size.height
                    val alphaEnd = if (node.isLastChild) 0.06f else 0.12f

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(threadLineColor, threadLineColor.copy(alpha = alphaEnd)),
                            startY = 0f,
                            endY = bottomY
                        ),
                        style = Stroke(width = strokeWidth)
                    )

                    // Continue line downward if not the last child
                    if (!node.isLastChild) {
                        drawLine(
                            color = threadLineColor,
                            start = Offset(parentX, elbowY),
                            end = Offset(parentX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // If this node has children, start the line dropping from its avatar
                if (node.hasChildren) {
                    val myX = startX + (node.depth * spacing)
                    val startY = 48.dp.toPx()
                    drawLine(
                        color = threadLineColor,
                        start = Offset(myX, startY),
                        end = Offset(myX, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(start = paddingStart, top = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Reply Icon (revealed when swiping right)
            Box(
                modifier = Modifier.matchParentSize().padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = (offsetX.value / replyThreshold).coerceIn(0f, 1f))
                )
}
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value > replyThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onReply(comment)
}
                                scope.launch {
                                    offsetX.animateTo(0f, spring(stiffness = 400f, dampingRatio = 0.6f))
}
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(0f, spring(stiffness = 400f, dampingRatio = 0.6f))
}
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (dragAmount > 0 || offsetX.value > 0) {
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo((offsetX.value + dragAmount * 0.5f).coerceAtLeast(0f).coerceAtMost(replyThreshold * 1.5f))
}
}
}
                        )
}
            ) {
                // Main comment bubble
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (node.depth > 0) VybPanelLifted.copy(alpha = .82f) else VybPanelLifted,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row {
                        SocialAvatar(
                            avatarUrl = comment.author?.avatarUrl.takeUnless { comment.isAnonymous },
                            displayName = if (comment.isAnonymous) "Anonymous Vyber" else comment.author?.displayName ?: "Vyber",
                            size = 36.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (comment.isAnonymous) "Anonymous Vyber" else comment.author?.displayName ?: "Vyber",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VybText
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (comment.isAnonymous) "" else "@${comment.author?.username ?: "vyber"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VybMuted
                                )
}
                            if (node.replyTargetUsername != null) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(VybPanel, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Replying to @${node.replyTargetUsername}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
}
}
                            Spacer(Modifier.height(4.dp))
                            Text(comment.body, style = MaterialTheme.typography.bodyMedium, color = VybText)
                            comment.mediaUrl?.let { commentMediaUrl ->
                                VybRemoteImage(
                                    url = commentMediaUrl,
                                    contentDescription = comment.mediaType ?: "Comment media",
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp))
                                )
                            }
                            // Reactions row
                            Row(
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    formatCommentDate(comment.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VybMuted
                                )
                                Spacer(Modifier.width(16.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isBusy) { onToggleReaction(comment.id) }
                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Like",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (comment.viewerHasLiked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (comment.viewerHasLiked) MaterialTheme.colorScheme.primary else VybMuted
                                    )
}
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "Reply",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VybMuted,
                                    modifier = Modifier.clickable { onReply(comment) }.padding(4.dp)
                                )
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(VybPanel, CircleShape)
                                        .clickable { menuOpen = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = VybMuted, modifier = Modifier.size(16.dp))
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        if (comment.viewerCanManage) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = { menuOpen = false; editOpen = true }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = { menuOpen = false; deleteOpen = true }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Report") },
                                                onClick = { menuOpen = false; reportOpen = true }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        // Replies are now flattened, so no recursive call needed here
    }
    if (editOpen) {
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text("Edit comment") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = false,
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editText.trim().length >= 2 && !isBusy,
                    onClick = { onUpdate(comment.id, editText.trim()); editOpen = false }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editOpen = false }) { Text("Cancel") } }
        )
    }
    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Delete comment?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(comment.id); deleteOpen = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Cancel") } }
        )
    }
    if (reportOpen) {
        AlertDialog(
            onDismissRequest = { reportOpen = false },
            title = { Text("Report comment?") },
            text = { Text("Vyb moderation will review this comment for harassment or harmful content.") },
            confirmButton = {
                TextButton(onClick = { onReport(comment.id, "harmful_content"); reportOpen = false }) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { reportOpen = false }) { Text("Cancel") } }
        )
    }
}
}
private fun formatCommentDate(value: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
}
        val date = parser.parse(value) ?: return ""
        val formatter = SimpleDateFormat("d MMM, h:mm a", Locale.US)
        formatter.format(date).lowercase(Locale.US)
    } catch (e: Exception) {
        value
}
}

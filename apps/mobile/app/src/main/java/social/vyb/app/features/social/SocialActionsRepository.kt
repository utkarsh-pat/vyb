package social.vyb.app.features.social

import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

class SocialActionsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: SocialActionsApi = VybNetwork.create()

    suspend fun createTextPost(
        text: String,
        isAnonymous: Boolean = false,
        allowAnonymousComments: Boolean = true,
        visibility: String = PostReach.Public.wireValue,
        communityId: String? = null
    ): SocialPost = apiCall {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Write something before publishing." }
        val bearer = bearerToken()
        val viewer = verifiedViewer(bearer)
        api.createPost(
            bearer,
            CreatePostBody(
                tenantId = viewer.tenantId,
                membershipId = viewer.id,
                body = trimmed,
                communityId = communityId,
                isAnonymous = isAnonymous,
                allowAnonymousComments = allowAnonymousComments,
                visibility = PostReach.fromWireValue(visibility).wireValue
            )
        ).item
    }

    suspend fun toggleReaction(postId: String, reactionType: String = "like"): ReactionResult =
        apiCall { api.toggleReaction(bearerToken(), postId, ReactionBody(reactionType)) }

    suspend fun toggleSave(postId: String): SaveResult =
        apiCall { api.toggleSave(bearerToken(), postId) }

    suspend fun listComments(postId: String): List<SocialComment> =
        apiCall { api.comments(bearerToken(), postId).items }

    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null,
        isAnonymous: Boolean = false
    ): SocialComment = apiCall {
        val trimmed = text.trim()
        require(trimmed.length >= 2) { "Comment must be at least 2 characters." }
        val bearer = bearerToken()
        val viewer = verifiedViewer(bearer)
        api.addComment(
            bearer,
            postId,
            CreateCommentBody(
                membershipId = viewer.id,
                body = trimmed,
                parentCommentId = parentCommentId,
                isAnonymous = isAnonymous
            )
        ).item
    }

    suspend fun toggleCommentReaction(commentId: String): CommentReactionResult =
        apiCall { api.toggleCommentReaction(bearerToken(), commentId) }

    suspend fun listReactionMembers(postId: String): List<ReactionMember> =
        apiCall { api.reactionMembers(bearerToken(), postId).items }

    suspend fun repost(
        postId: String,
        quote: String? = null,
        placement: String = "feed"
    ): SocialPost = apiCall {
        api.repost(
            bearerToken(),
            postId,
            RepostBody(
                quote = quote?.trim()?.takeIf(String::isNotEmpty),
                placement = placement.takeIf { it == "feed" || it == "vibe" } ?: "feed"
            )
        ).item
    }

    suspend fun updatePost(postId: String, title: String, body: String): SocialPost = apiCall {
        val trimmedBody = body.trim()
        require(trimmedBody.isNotEmpty()) { "Post cannot be empty." }
        api.updatePost(
            bearerToken(),
            postId,
            UpdatePostBody(title = title.trim(), body = trimmedBody)
        ).item
    }

    suspend fun deletePost(postId: String) = apiCall {
        api.deletePost(bearerToken(), postId)
    }

    suspend fun updateComment(commentId: String, body: String): SocialComment = apiCall {
        val trimmed = body.trim()
        require(trimmed.length >= 2) { "Comment must be at least 2 characters." }
        api.updateComment(bearerToken(), commentId, UpdateCommentBody(trimmed)).item
    }

    suspend fun deleteComment(commentId: String) = apiCall {
        api.deleteComment(bearerToken(), commentId)
    }

    internal suspend fun report(targetType: String, targetId: String, reason: String) = apiCall {
        val trimmed = reason.trim()
        require(trimmed.isNotEmpty()) { "Choose or enter a report reason." }
        api.report(
            bearerToken(),
            ReportBody(targetType = targetType, targetId = targetId, reason = trimmed)
        ).item
    }

    private suspend fun verifiedViewer(bearer: String): ViewerMembership {
        val membership = api.viewer(bearer).membershipSummary
        check(membership.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }
        return membership
    }

    private suspend fun bearerToken(): String = auth.requireBearerToken()

    private suspend fun <T> apiCall(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val responseBody = error.response()?.errorBody()?.string()
        val backendMessage = responseBody?.let {
            runCatching {
                VybNetwork.json.decodeFromString<ErrorEnvelope>(it).error?.message
            }.getOrNull()
        }
        throw SocialActionException(
            backendMessage ?: "Request failed (${error.code()}). Please try again.",
            error
        )
    }
}

class SocialActionException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

@Serializable
private data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
private data class ErrorBody(val message: String? = null)

package social.vyb.app.features.search

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import social.vyb.app.data.FeedEnvelope
import social.vyb.app.data.MeEnvelope
import social.vyb.app.data.RemotePost
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken
import social.vyb.app.data.network.requireIdToken
import social.vyb.app.features.market.MarketDashboard
import social.vyb.app.features.market.MarketListing
import social.vyb.app.features.market.MarketRequest

internal interface SearchApi {
    @GET("v1/me")
    suspend fun viewer(@Header("Authorization") bearer: String): MeEnvelope

    @GET("v1/feed")
    suspend fun posts(
        @Header("Authorization") bearer: String,
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 50
    ): FeedEnvelope

    @GET("v1/vibes")
    suspend fun vibes(
        @Header("Authorization") bearer: String,
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 50
    ): FeedEnvelope

    @GET("v1/market")
    suspend fun market(@Header("Authorization") bearer: String): MarketDashboard

    @GET("v1/users/search")
    suspend fun search(
        @Header("Authorization") bearer: String,
        @Query("q") query: String? = null,
        @Query("suggested") suggested: Int? = null,
        @Query("limit") limit: Int = 20
    ): UserSearchResponse

    @GET("v1/users/{username}")
    suspend fun profile(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): PublicProfileResponse

    @PUT("v1/users/{username}/follow")
    suspend fun follow(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): FollowResponse

    @DELETE("v1/users/{username}/follow")
    suspend fun unfollow(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): FollowResponse

    @PUT("v1/users/{username}/block")
    suspend fun block(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): BlockResponse

    @DELETE("v1/users/{username}/block")
    suspend fun unblock(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): BlockResponse

    @GET("v1/users/blocked")
    suspend fun blocked(
        @Header("Authorization") bearer: String,
        @Query("limit") limit: Int = 50
    ): BlockedPeopleResponse
}

class SearchRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: SearchApi = VybNetwork.create()
    private val discoveryMutex = Mutex()

    @Volatile
    private var discoveryCache: DiscoveryCache? = null

    suspend fun suggested(): List<CampusPerson> =
        api.search(bearer(), suggested = 1, limit = 12).items

    suspend fun discover(): UniversalSearchResult {
        val user = auth.currentUser ?: error("Your session expired. Please sign in again.")
        val authorization = "Bearer ${user.requireIdToken()}"
        val discovery = loadDiscovery(uid = user.uid, bearer = authorization)
        return UniversalSearchResult(
            posts = discovery.posts.take(30).map(RemotePost::toSearchContent),
            vibes = discovery.vibes.take(30).map(RemotePost::toSearchContent),
            marketplace = discovery.marketplace.take(30),
            categoryErrors = discovery.errors
        )
    }

    suspend fun search(query: String): UniversalSearchResult = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@coroutineScope UniversalSearchResult()
        val user = auth.currentUser ?: error("Your session expired. Please sign in again.")
        val authorization = "Bearer ${user.requireIdToken()}"
        val peopleRequest = async {
            runCatching { api.search(authorization, query = trimmed, limit = 30).items }
        }
        val discoveryRequest = async {
            loadDiscovery(uid = user.uid, bearer = authorization)
        }
        val people = peopleRequest.await()
        val discovery = discoveryRequest.await()
        val errors = discovery.errors.toMutableMap()
        if (people.isFailure) {
            errors[SearchCategory.People] = people.exceptionOrNull()?.message
                ?: "People search is unavailable."
        }
        UniversalSearchResult(
            people = people.getOrDefault(emptyList()),
            posts = discovery.posts
                .filter { it.matchesSearchQuery(trimmed) }
                .take(30)
                .map(RemotePost::toSearchContent),
            vibes = discovery.vibes
                .filter { it.matchesSearchQuery(trimmed) }
                .take(30)
                .map(RemotePost::toSearchContent),
            marketplace = discovery.marketplace
                .filter { it.matchesSearchQuery(trimmed) }
                .take(30),
            categoryErrors = errors
        )
    }

    suspend fun profile(username: String): PublicProfileResponse =
        api.profile(bearer(), username)

    internal suspend fun setFollowing(username: String, following: Boolean): FollowResponse =
        if (following) api.follow(bearer(), username) else api.unfollow(bearer(), username)

    suspend fun setBlocked(username: String, blocked: Boolean): BlockResponse =
        if (blocked) api.block(bearer(), username) else api.unblock(bearer(), username)

    suspend fun blockedPeople(): List<BlockedPerson> = api.blocked(bearer()).items

    fun invalidateDiscovery() {
        discoveryCache = null
    }

    private suspend fun bearer(): String = auth.requireBearerToken()

    private suspend fun loadDiscovery(uid: String, bearer: String): DiscoveryCache {
        val now = System.currentTimeMillis()
        discoveryCache?.takeIf { it.uid == uid && now - it.loadedAtMillis < CACHE_TTL_MS }
            ?.let { return it }
        return discoveryMutex.withLock {
            discoveryCache?.takeIf {
                it.uid == uid && now - it.loadedAtMillis < CACHE_TTL_MS
            } ?: coroutineScope {
                val viewerRequest = async { runCatching { api.viewer(bearer) } }
                val marketRequest = async { runCatching { api.market(bearer) } }
                val viewer = viewerRequest.await()
                val tenantId = viewer.getOrNull()?.membershipSummary?.tenantId
                val postsRequest = async<Result<List<RemotePost>>> {
                    if (tenantId == null) {
                        Result.failure(IllegalStateException("Campus feed scope is unavailable."))
                    } else {
                        runCatching { api.posts(bearer, tenantId).items }
                    }
                }
                val vibesRequest = async<Result<List<RemotePost>>> {
                    if (tenantId == null) {
                        Result.failure(IllegalStateException("Campus vibe scope is unavailable."))
                    } else {
                        runCatching { api.vibes(bearer, tenantId).items }
                    }
                }
                val posts = postsRequest.await()
                val vibes = vibesRequest.await()
                val market = marketRequest.await()
                buildMap<SearchCategory, String> {
                    posts.exceptionOrNull()?.let {
                        put(SearchCategory.Posts, it.message ?: "Posts search is unavailable.")
                    }
                    vibes.exceptionOrNull()?.let {
                        put(SearchCategory.Vibes, it.message ?: "Vibes search is unavailable.")
                    }
                    market.exceptionOrNull()?.let {
                        put(
                            SearchCategory.Marketplace,
                            it.message ?: "Marketplace search is unavailable."
                        )
                    }
                }.let { errors ->
                    DiscoveryCache(
                        uid = uid,
                        loadedAtMillis = System.currentTimeMillis(),
                        posts = posts.getOrDefault(emptyList()),
                        vibes = vibes.getOrDefault(emptyList()),
                        marketplace = market.getOrNull()?.toSearchItems().orEmpty(),
                        errors = errors
                    ).also { discoveryCache = it }
                }
            }
        }
    }

    private companion object {
        const val CACHE_TTL_MS = 60_000L
    }
}

private data class DiscoveryCache(
    val uid: String,
    val loadedAtMillis: Long,
    val posts: List<RemotePost>,
    val vibes: List<RemotePost>,
    val marketplace: List<MarketSearchItem>,
    val errors: Map<SearchCategory, String>
)

internal fun RemotePost.matchesSearchQuery(query: String): Boolean =
    matchesSearchQuery(
        query = query,
        values = listOf(
            title,
            body,
            author.displayName,
            author.username,
            location.orEmpty()
        )
    )

internal fun RemotePost.toSearchContent() = SearchContentItem(
    id = id,
    title = title,
    body = body,
    authorName = if (isAnonymous || author.isAnonymous) "Anonymous" else author.displayName,
    authorUsername = if (isAnonymous || author.isAnonymous) "anonymous" else author.username,
    mediaUrl = media.firstOrNull()?.url ?: mediaUrl,
    mediaKind = media.firstOrNull()?.kind ?: kind,
    location = location,
    reactionCount = reactions,
    commentCount = comments,
    createdAt = createdAt
)

internal fun MarketSearchItem.matchesSearchQuery(query: String): Boolean =
    matchesSearchQuery(
        query = query,
        values = listOf(title, description, category, location, ownerName, priceLabel)
    )

internal fun matchesSearchQuery(query: String, values: List<String>): Boolean {
    val tokens = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotBlank)
    if (tokens.isEmpty()) return false
    val searchable = values.joinToString(" ").lowercase()
    return tokens.all(searchable::contains)
}

private fun MarketDashboard.toSearchItems(): List<MarketSearchItem> =
    listings.map(MarketListing::toSearchItem) + requests.map(MarketRequest::toSearchItem)

private fun MarketListing.toSearchItem() = MarketSearchItem(
    id = id,
    kind = MarketSearchKind.Listing,
    title = title,
    description = description,
    category = category,
    priceLabel = if (priceAmount > 0) "Rs $priceAmount" else "Contact seller",
    location = campusSpot.ifBlank { location },
    ownerName = seller.displayName.ifBlank { seller.username },
    ownerUsername = seller.username,
    mediaUrl = media.firstOrNull()?.url,
    createdAt = createdAt
)

private fun MarketRequest.toSearchItem() = MarketSearchItem(
    id = id,
    kind = MarketSearchKind.Request,
    title = title,
    description = detail,
    category = category,
    priceLabel = budgetLabel.ifBlank {
        budgetAmount?.let { "Rs $it" } ?: "Flexible budget"
    },
    location = campusSpot,
    ownerName = requester.displayName.ifBlank { requester.username },
    ownerUsername = requester.username,
    mediaUrl = media.firstOrNull()?.url,
    createdAt = createdAt
)

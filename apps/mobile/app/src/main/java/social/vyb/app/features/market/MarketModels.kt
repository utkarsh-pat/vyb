package social.vyb.app.features.market

import kotlinx.serialization.Serializable

@Serializable
data class MarketActor(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val role: String = "student",
)

@Serializable
data class MarketMedia(
    val id: String = "",
    val kind: String = "image",
    val url: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val storagePath: String? = null,
)

@Serializable
data class MarketListing(
    val id: String = "",
    val tenantId: String = "",
    val seller: MarketActor = MarketActor(),
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val condition: String = "",
    val priceAmount: Long = 0,
    val location: String = "",
    val campusSpot: String = "",
    val media: List<MarketMedia> = emptyList(),
    val createdAt: String = "",
    val savedCount: Int = 0,
    val inquiryCount: Int = 0,
    val isSaved: Boolean = false,
)

@Serializable
data class MarketRequest(
    val id: String = "",
    val tenantId: String = "",
    val tab: String = "buying",
    val requester: MarketActor = MarketActor(),
    val tag: String = "",
    val title: String = "",
    val detail: String = "",
    val category: String = "",
    val campusSpot: String = "",
    val media: List<MarketMedia> = emptyList(),
    val budgetLabel: String = "",
    val budgetAmount: Long? = null,
    val tone: String = "violet",
    val createdAt: String = "",
    val responseCount: Int = 0,
)

@Serializable
data class MarketViewer(
    val userId: String = "",
    val username: String = "",
    val savedCount: Int = 0,
)

@Serializable
data class MarketDashboard(
    val tenantId: String = "",
    val viewer: MarketViewer = MarketViewer(),
    val listings: List<MarketListing> = emptyList(),
    val requests: List<MarketRequest> = emptyList(),
    val viewerActiveListings: List<MarketListing> = emptyList(),
    val viewerActiveRequests: List<MarketRequest> = emptyList(),
)

@Serializable
data class CreateMarketPost(
    val tab: String,
    val title: String,
    val category: String,
    val description: String,
    val location: String? = null,
    val campusSpot: String? = null,
    val condition: String? = null,
    val priceAmount: Long? = null,
    val budgetAmount: Long? = null,
    val budgetLabel: String? = null,
    val tag: String? = null,
)

@Serializable
internal data class SaveMarketListing(val listingId: String)

@Serializable
internal data class ContactMarketPost(
    val targetId: String,
    val targetType: String,
    val message: String,
)

@Serializable
internal data class MarketMutationResponse(
    val dashboard: MarketDashboard,
    val itemId: String? = null,
    val itemType: String? = null,
    val listingId: String? = null,
    val requestId: String? = null,
    val targetId: String? = null,
    val targetType: String? = null,
    val isSaved: Boolean? = null,
    val action: String? = null,
)

data class MarketPostDraft(
    val tab: String,
    val title: String,
    val category: String,
    val description: String,
    val amount: Long?,
    val campusSpot: String,
    val condition: String,
)

sealed interface MarketDetail {
    val id: String

    data class Listing(val value: MarketListing) : MarketDetail {
        override val id: String = value.id
    }

    data class Request(val value: MarketRequest) : MarketDetail {
        override val id: String = value.id
    }
}

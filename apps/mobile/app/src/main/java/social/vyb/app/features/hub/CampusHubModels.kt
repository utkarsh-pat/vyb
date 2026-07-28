package social.vyb.app.features.hub

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class HubEventActor(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "Campus host",
    val role: String = "student"
)

@Serializable
data class HubEventRegistrationSummary(
    val id: String = "",
    val status: String = "submitted",
    val submittedAt: String = "",
    val updatedAt: String = "",
    val teamName: String? = null,
    val teamSize: Int = 1,
    val note: String? = null,
    val reviewNote: String? = null,
    val attachmentCount: Int = 0
)

@Serializable
data class HubEvent(
    val id: String,
    val tenantId: String = "",
    val communityId: String? = null,
    val host: HubEventActor = HubEventActor(),
    val title: String,
    val club: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val startsAt: String = "",
    val endsAt: String? = null,
    val passKind: String = "free",
    val passLabel: String = "Free",
    val capacity: Int? = null,
    val spotsLeft: Int? = null,
    val isRegistrationOpen: Boolean = false,
    val status: String = "published",
    val savedCount: Int = 0,
    val interestCount: Int = 0,
    val responseMode: String = "register",
    val viewerRegistration: HubEventRegistrationSummary? = null,
    val isSaved: Boolean = false,
    val isInterested: Boolean = false,
    val isHostedByViewer: Boolean = false
)

@Serializable
data class EventsDashboardDto(
    val tenantId: String = "",
    val events: List<HubEvent> = emptyList(),
    val hostedEvents: List<HubEvent> = emptyList(),
    val categories: List<String> = emptyList()
)

@Serializable
data class EventEnvelopeDto(@SerialName("event") val item: HubEvent)

@Serializable
data class EventMutationDto(
    val dashboard: EventsDashboardDto,
    val eventId: String = "",
    val isSaved: Boolean? = null
)

@Serializable
data class RegisterEventRequestDto(
    val eventId: String,
    val note: String? = null,
    val teamMembers: List<String> = emptyList(),
    val answers: List<String> = emptyList()
)

@Serializable
data class EventRegistrationMutationDto(
    val dashboard: EventsDashboardDto,
    val event: HubEvent,
    val registration: HubEventRegistrationSummary
)

@Serializable
data class ResourceFileDto(
    val id: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val storagePath: String? = null,
    val url: String? = null
)

@Serializable
data class HubResource(
    val id: String,
    val tenantId: String = "",
    val membershipId: String = "",
    val courseId: String? = null,
    val communityId: String? = null,
    val title: String,
    val description: String = "",
    val type: String = "notes",
    val downloads: Int = 0,
    val status: String = "published",
    val createdAt: String = "",
    val files: List<ResourceFileDto> = emptyList()
)

@Serializable
data class ResourcesResponseDto(
    val tenantId: String = "",
    val courseId: String? = null,
    val communityId: String? = null,
    val items: List<HubResource> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class HubCommunity(
    val id: String,
    val name: String,
    val type: String = "general",
    val memberCount: Int = 0,
    val slug: String = "",
    val visibility: String? = null,
    val membershipRole: String? = null,
    val isOfficial: Boolean = false,
    val isMember: Boolean = true,
    val muted: Boolean = false,
    val pinned: Boolean = false,
    val membershipStatus: String? = null
)

@Serializable
data class CommunitiesResponseDto(val communities: List<HubCommunity> = emptyList())

@Serializable
data class CommunityViewerDto(
    val isMember: Boolean = false,
    val role: String? = null,
    val membershipStatus: String? = null
)

@Serializable
data class CommunitySummaryDto(
    val postCount: Int? = null,
    val resourceCount: Int? = null,
    val eventCount: Int? = null,
    val health: String = "normal"
)

@Serializable
data class CommunityDetailDto(
    val community: HubCommunity,
    val viewer: CommunityViewerDto = CommunityViewerDto(),
    val summary: CommunitySummaryDto = CommunitySummaryDto()
)

@Serializable
data class HubCommunityMember(
    val membershipId: String,
    val userId: String = "",
    val username: String? = null,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: String = "member",
    val course: String? = null,
    val branch: String? = null,
    val batchYear: Int? = null,
    val section: String? = null,
    val hostel: String? = null,
    val joinedAt: String = ""
)

@Serializable
data class CommunityMembersDto(
    val items: List<HubCommunityMember> = emptyList(),
    val nextCursor: String? = null
)

enum class CampusHubTab(val title: String) {
    Events("Events"),
    Resources("Resources"),
    Communities("Communities")
}

data class CampusHubUiState(
    val selectedTab: CampusHubTab = CampusHubTab.Events,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val events: List<HubEvent> = emptyList(),
    val resources: List<HubResource> = emptyList(),
    val communities: List<HubCommunity> = emptyList(),
    val selectedEvent: HubEvent? = null,
    val selectedCommunity: CommunityDetailDto? = null,
    val communityMembers: List<HubCommunityMember> = emptyList(),
    val busyId: String? = null
)

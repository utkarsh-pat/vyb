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
data class HubEventMediaAsset(
    val id: String = "",
    val kind: String = "image",
    val url: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val storagePath: String? = null,
)

@Serializable
data class HubEventFormField(
    val id: String = "",
    val label: String = "",
    val type: String = "short_text",
    val required: Boolean = false,
    val placeholder: String? = null,
    val helpText: String? = null,
    val options: List<String> = emptyList(),
)

@Serializable
data class HubEventRegistrationConfig(
    val mode: String = "register",
    val entryMode: String = "individual",
    val closesAt: String? = null,
    val requiresApproval: Boolean = false,
    val teamSizeMin: Int? = null,
    val teamSizeMax: Int? = null,
    val allowAttachments: Boolean = false,
    val attachmentLabel: String? = null,
    val formFields: List<HubEventFormField> = emptyList(),
)

@Serializable
data class HubEventRegistrationCounts(
    val total: Int = 0,
    val submitted: Int = 0,
    val approved: Int = 0,
    val waitlisted: Int = 0,
    val rejected: Int = 0,
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
    val media: List<HubEventMediaAsset> = emptyList(),
    val passKind: String = "free",
    val passLabel: String = "Free",
    val capacity: Int? = null,
    val spotsLeft: Int? = null,
    val isRegistrationOpen: Boolean = false,
    val commentCount: Int = 0,
    val status: String = "published",
    val createdAt: String = "",
    val savedCount: Int = 0,
    val interestCount: Int = 0,
    val responseMode: String = "register",
    val registrationConfig: HubEventRegistrationConfig = HubEventRegistrationConfig(),
    val registrationSummary: HubEventRegistrationCounts = HubEventRegistrationCounts(),
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
data class UpsertHubEventRequest(
    val communityId: String? = null,
    val title: String,
    val club: String,
    val category: String,
    val description: String,
    val location: String,
    val startsAt: String,
    val endsAt: String? = null,
    val passKind: String,
    val passLabel: String? = null,
    val capacity: Int? = null,
    val responseMode: String,
    val registrationClosesAt: String? = null,
    val entryMode: String? = null,
    val teamSizeMin: Int? = null,
    val teamSizeMax: Int? = null,
    val allowAttachments: Boolean = false,
    val attachmentLabel: String? = null,
    val formFields: List<HubEventFormField> = emptyList(),
    val keepMediaIds: List<String> = emptyList(),
    val media: List<HubEventMediaAsset> = emptyList(),
)

data class HubEventHostDraft(
    val title: String = "",
    val club: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val startsAt: String = "",
    val endsAt: String = "",
    val passKind: String = "free",
    val passLabel: String = "Free",
    val capacity: String = "",
    val responseMode: String = "register",
    val registrationClosesAt: String = "",
)

internal fun HubEventHostDraft.validationError(nowEpochMillis: Long = System.currentTimeMillis()): String? {
    if (title.trim().length < 3) return "Add an event title with at least 3 characters."
    if (club.isBlank()) return "Add the hosting club or community."
    if (category.isBlank()) return "Choose an event category."
    if (description.trim().length < 10) return "Add a useful event description."
    if (location.isBlank()) return "Add an event location."
    val start = runCatching { java.time.Instant.parse(startsAt.trim()) }.getOrNull()
        ?: return "Start time must use ISO format, for example 2026-08-15T10:00:00Z."
    val end = endsAt.trim().takeIf(String::isNotBlank)?.let {
        runCatching { java.time.Instant.parse(it) }.getOrNull()
            ?: return "End time must be a valid ISO timestamp."
    }
    if (start.toEpochMilli() <= nowEpochMillis) return "Start time must be in the future."
    if (end != null && !end.isAfter(start)) return "End time must be after the start time."
    if (passKind !in setOf("free", "rsvp", "paid")) return "Choose a valid pass type."
    if (responseMode !in setOf("interest", "register", "apply")) return "Choose a valid response mode."
    if (capacity.isNotBlank() && (capacity.toIntOrNull() ?: 0) <= 0) return "Capacity must be a positive number."
    val closes = registrationClosesAt.trim().takeIf(String::isNotBlank)?.let {
        runCatching { java.time.Instant.parse(it) }.getOrNull()
            ?: return "Registration close time must be a valid ISO timestamp."
    }
    if (closes != null && !closes.isBefore(start)) return "Registration must close before the event starts."
    return null
}

internal fun HubEventHostDraft.toRequest(existing: HubEvent? = null): UpsertHubEventRequest =
    UpsertHubEventRequest(
        communityId = existing?.communityId,
        title = title.trim(),
        club = club.trim(),
        category = category.trim(),
        description = description.trim(),
        location = location.trim(),
        startsAt = startsAt.trim(),
        endsAt = endsAt.trim().takeIf(String::isNotBlank),
        passKind = passKind,
        passLabel = passLabel.trim().takeIf(String::isNotBlank),
        capacity = capacity.toIntOrNull(),
        responseMode = responseMode,
        registrationClosesAt = registrationClosesAt.trim().takeIf(String::isNotBlank),
        entryMode = existing?.registrationConfig?.entryMode ?: "individual",
        teamSizeMin = existing?.registrationConfig?.teamSizeMin,
        teamSizeMax = existing?.registrationConfig?.teamSizeMax,
        allowAttachments = existing?.registrationConfig?.allowAttachments ?: false,
        attachmentLabel = existing?.registrationConfig?.attachmentLabel,
        formFields = existing?.registrationConfig?.formFields.orEmpty(),
        keepMediaIds = existing?.media.orEmpty().map(HubEventMediaAsset::id).filter(String::isNotBlank),
        media = emptyList(),
    )

internal fun HubEvent.toHostDraft() = HubEventHostDraft(
    title = title,
    club = club,
    category = category,
    description = description,
    location = location,
    startsAt = startsAt,
    endsAt = endsAt.orEmpty(),
    passKind = passKind,
    passLabel = passLabel,
    capacity = capacity?.toString().orEmpty(),
    responseMode = responseMode,
    registrationClosesAt = registrationConfig.closesAt.orEmpty(),
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
data class HubEventRegistrationAnswer(
    val fieldId: String = "",
    val label: String = "",
    val value: String = "",
)

@Serializable
data class HubEventTeamMember(
    val id: String = "",
    val name: String = "",
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    val role: String? = null,
)

@Serializable
data class HubEventRegistration(
    val id: String,
    val eventId: String = "",
    val attendee: HubEventActor = HubEventActor(),
    val status: String = "submitted",
    val submittedAt: String = "",
    val updatedAt: String = "",
    val teamName: String? = null,
    val teamSize: Int = 1,
    val teamMembers: List<HubEventTeamMember> = emptyList(),
    val answers: List<HubEventRegistrationAnswer> = emptyList(),
    val attachments: List<HubEventMediaAsset> = emptyList(),
    val note: String? = null,
    val reviewNote: String? = null,
)

@Serializable
data class HubEventRegistrationListDto(
    val event: HubEvent,
    val registrations: List<HubEventRegistration> = emptyList(),
)

@Serializable
data class ManageHubEventRegistrationRequest(
    val status: String,
    val reviewNote: String? = null,
)

@Serializable
data class ManageHubEventRegistrationDto(
    val dashboard: EventsDashboardDto,
    val event: HubEvent,
    val registrations: List<HubEventRegistration> = emptyList(),
    val registrationId: String = "",
    val status: String = "",
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
    val busyId: String? = null,
    val hostEditorOpen: Boolean = false,
    val hostEditorEvent: HubEvent? = null,
    val registrationAdminEvent: HubEvent? = null,
    val hostRegistrations: List<HubEventRegistration> = emptyList(),
    val registrationsLoading: Boolean = false,
)

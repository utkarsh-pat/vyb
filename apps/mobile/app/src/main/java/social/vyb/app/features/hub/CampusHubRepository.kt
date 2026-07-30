package social.vyb.app.features.hub

import com.google.firebase.auth.FirebaseAuth
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

class CampusHubRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: CampusHubApi = VybNetwork.create()

    suspend fun loadEvents(): List<HubEvent> = call {
        val dashboard = api.events(bearer())
        (dashboard.events + dashboard.hostedEvents).distinctBy(HubEvent::id)
    }

    suspend fun loadEvent(eventId: String): HubEvent = call {
        api.event(bearer(), eventId).item
    }

    suspend fun createEvent(draft: HubEventHostDraft): Pair<List<HubEvent>, String> = call {
        draft.validationError()?.let { throw IllegalArgumentException(it) }
        val response = api.createEvent(bearer(), draft.toRequest())
        response.dashboard.mergedEvents() to response.eventId
    }

    suspend fun updateEvent(event: HubEvent, draft: HubEventHostDraft): Pair<List<HubEvent>, String> = call {
        draft.validationError()?.let { throw IllegalArgumentException(it) }
        val response = api.updateEvent(bearer(), event.id, draft.toRequest(event))
        response.dashboard.mergedEvents() to response.eventId
    }

    suspend fun toggleEventSave(eventId: String): List<HubEvent> = call {
        val dashboard = api.toggleEventSave(bearer(), eventId).dashboard
        (dashboard.events + dashboard.hostedEvents).distinctBy(HubEvent::id)
    }

    suspend fun registerEvent(eventId: String): List<HubEvent> = call {
        val dashboard = api.registerEvent(
            bearer(),
            eventId,
            RegisterEventRequestDto(eventId = eventId)
        ).dashboard
        dashboard.mergedEvents()
    }

    suspend fun loadRegistrations(eventId: String): Pair<HubEvent, List<HubEventRegistration>> = call {
        val response = api.registrations(bearer(), eventId)
        response.event to response.registrations.sortedByDescending(HubEventRegistration::updatedAt)
    }

    suspend fun manageRegistration(
        eventId: String,
        registrationId: String,
        status: String,
        reviewNote: String?,
    ): Triple<List<HubEvent>, HubEvent, List<HubEventRegistration>> = call {
        require(status in setOf("approved", "waitlisted", "rejected")) {
            "Choose approved, waitlisted, or rejected."
        }
        val response = api.manageRegistration(
            bearer(),
            eventId,
            registrationId,
            ManageHubEventRegistrationRequest(
                status = status,
                reviewNote = reviewNote?.trim()?.takeIf(String::isNotBlank),
            ),
        )
        Triple(
            response.dashboard.mergedEvents(),
            response.event,
            response.registrations,
        )
    }

    suspend fun loadResources(): List<HubResource> = call {
        val token = bearer()
        val tenantId = api.me(token).membershipSummary.tenantId
        api.resources(token, tenantId).items.filter { it.status == "published" }
    }

    suspend fun loadCommunities(): List<HubCommunity> = call {
        api.communities(bearer()).communities
    }

    suspend fun loadCommunity(slug: String): Pair<CommunityDetailDto, List<HubCommunityMember>> = call {
        val token = bearer()
        val detail = api.community(token, slug)
        val members = api.communityMembers(token, slug).items
        detail to members
    }

    private suspend fun bearer(): String = auth.requireBearerToken()

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val message = when (error.code()) {
            400 -> "This action needs more information."
            401 -> "Your session expired. Please sign in again."
            403 -> "Complete your campus profile to access this section."
            404 -> "This campus item is no longer available."
            429 -> "Too many requests. Please try again shortly."
            in 500..599 -> "Campus Hub is temporarily unavailable."
            else -> "Campus Hub could not connect (${error.code()})."
        }
        throw IllegalStateException(message, error)
    }
}

private fun EventsDashboardDto.mergedEvents(): List<HubEvent> =
    (events + hostedEvents).distinctBy(HubEvent::id)

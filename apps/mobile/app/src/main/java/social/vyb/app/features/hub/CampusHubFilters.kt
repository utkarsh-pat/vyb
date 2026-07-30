package social.vyb.app.features.hub

internal fun filterHubEvents(
    events: List<HubEvent>,
    query: String,
    scope: String,
    category: String
): List<HubEvent> {
    val normalized = query.trim().lowercase()
    return events.filter { event ->
        val inScope = when (scope) {
            "Saved" -> event.isSaved
            "Ended" -> event.status == "ended" || event.status == "completed"
            "Hosting" -> event.isHostedByViewer
            "Registered" -> event.viewerRegistration != null || event.isInterested
            else -> event.status == "published"
        }
        val matches = normalized.isEmpty() ||
            listOf(
                event.title,
                event.club,
                event.description,
                event.location,
                event.category,
                event.host.username
            ).any { normalized in it.lowercase() }
        inScope && (category == "All" || event.category == category) && matches
    }
}

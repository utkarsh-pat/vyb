package social.vyb.app.ui

internal data class ProfileDraft(
    val username: String,
    val firstName: String,
    val course: String,
    val stream: String,
    val year: Int?,
    val section: String,
    val isHosteller: Boolean,
    val hostelName: String
)

internal fun validateProfileDraft(draft: ProfileDraft): String? = when {
    !draft.username.matches(Regex("^[a-z0-9](?:[a-z0-9._]{1,22}[a-z0-9])?$")) ->
        "User ID must be 3–24 characters using letters, numbers, dots, or underscores."
    draft.firstName.trim().length < 2 -> "First name must be at least 2 characters."
    draft.course.trim().length < 2 || draft.stream.trim().length < 2 ->
        "Course and stream are required."
    draft.year !in 1..6 -> "Year must be between 1 and 6."
    draft.section.isBlank() -> "Section is required."
    draft.isHosteller && draft.hostelName.isBlank() ->
        "Hostel name is required for hostellers."
    else -> null
}

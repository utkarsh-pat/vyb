package social.vyb.app.features.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import social.vyb.app.data.ProfileRecord
import social.vyb.app.data.UpsertProfileRequest
import social.vyb.app.features.search.PublicProfileResponse
import social.vyb.app.features.social.SocialPost
import social.vyb.app.features.social.SavedPostsSync

enum class ProfilePanel {
    Overview,
    Edit,
    Settings,
    Privacy,
    Security,
    Connections
}

data class ProfileEditDraft(
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val course: String = "",
    val stream: String = "",
    val year: String = "",
    val section: String = "",
    val isHosteller: Boolean = false,
    val hostelName: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val linkedin: String = "",
    val github: String = "",
    val instagram: String = "",
    val avatarUrl: String? = null
) {
    fun toRequest(): UpsertProfileRequest {
        require(username.matches(Regex("^[a-z0-9](?:[a-z0-9._]{1,22}[a-z0-9])?$"))) {
            "User ID must be 3–24 characters using lowercase letters, numbers, dots, or underscores."
        }
        require(firstName.trim().length >= 2) { "First name must be at least 2 characters." }
        require(course.trim().length >= 2 && stream.trim().length >= 2) {
            "Course and stream are required."
        }
        val parsedYear = year.toIntOrNull()
        require(parsedYear in 1..6) { "Year must be between 1 and 6." }
        require(section.isNotBlank()) { "Section is required." }
        require(!isHosteller || hostelName.isNotBlank()) {
            "Hostel name is required for hostellers."
        }
        val links = mapOf(
            "linkedin" to linkedin.trim(),
            "github" to github.trim(),
            "instagram" to instagram.trim()
        ).filterValues(String::isNotBlank)
        return UpsertProfileRequest(
            username = username.trim(),
            firstName = firstName.trim(),
            lastName = lastName.trim().ifBlank { null },
            course = course.trim(),
            stream = stream.trim(),
            year = requireNotNull(parsedYear),
            section = section.trim().uppercase(),
            isHosteller = isHosteller,
            hostelName = hostelName.trim().takeIf { isHosteller && it.isNotBlank() },
            phoneNumber = phoneNumber.trim().ifBlank { null },
            bio = bio.trim().ifBlank { null },
            socialLinks = links.ifEmpty { null },
            avatarUrl = avatarUrl
        )
    }

    companion object {
        fun from(profile: ProfileRecord) = ProfileEditDraft(
            username = profile.username,
            firstName = profile.firstName,
            lastName = profile.lastName.orEmpty(),
            course = profile.course,
            stream = profile.stream,
            year = profile.year.toString(),
            section = profile.section,
            isHosteller = profile.isHosteller,
            hostelName = profile.hostelName.orEmpty(),
            phoneNumber = profile.phoneNumber.orEmpty(),
            bio = profile.bio.orEmpty(),
            linkedin = profile.socialLinks?.get("linkedin").orEmpty(),
            github = profile.socialLinks?.get("github").orEmpty(),
            instagram = profile.socialLinks?.get("instagram").orEmpty(),
            avatarUrl = profile.avatarUrl
        )
    }
}

data class ProfileUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val privateProfile: ProfileRecord? = null,
    val publicProfile: PublicProfileResponse? = null,
    val privacy: ChatPrivacySettings = ChatPrivacySettings(),
    val contentMeasurementEnabled: Boolean = true,
    val devices: List<TrustedDevice> = emptyList(),
    val panel: ProfilePanel = ProfilePanel.Overview,
    val activeTab: String = "posts",
    val savedPosts: List<SocialPost> = emptyList(),
    val savedLoading: Boolean = false,
    val savedLoaded: Boolean = false,
    val savedError: String? = null,
    val connectionScope: String = "followers",
    val connections: List<ProfileConnection> = emptyList(),
    val editDraft: ProfileEditDraft = ProfileEditDraft(),
    val error: String? = null,
    val notice: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        refresh()
        val initialSavedRevision = SavedPostsSync.changes.value.revision
        viewModelScope.launch {
            SavedPostsSync.changes
                .filter { it.revision > initialSavedRevision }
                .collect { change ->
                    _state.update { current ->
                        current.copy(
                            savedPosts = if (!change.isSaved) {
                                current.savedPosts.filterNot { it.id == change.postId }
                            } else {
                                current.savedPosts
                            },
                            savedLoaded = false,
                            savedError = null,
                        )
                    }
                    if (_state.value.activeTab == "saved") {
                        loadSavedPosts(force = true)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.load() }
                .onSuccess { bundle ->
                    val profile = requireNotNull(bundle.privateProfile.profile)
                    _state.update {
                        it.copy(
                            loading = false,
                            privateProfile = profile,
                            publicProfile = bundle.publicProfile,
                            privacy = bundle.privacy,
                            devices = bundle.devices,
                            contentMeasurementEnabled = bundle.contentMeasurementEnabled,
                            editDraft = ProfileEditDraft.from(profile),
                            error = null
                        )
                    }
                }
                .onFailure { fail(it, "Profile could not be loaded.") }
        }
    }

    fun open(panel: ProfilePanel) {
        _state.update { it.copy(panel = panel, error = null, notice = null) }
    }

    fun back() {
        val destination = when (_state.value.panel) {
            ProfilePanel.Privacy, ProfilePanel.Security -> ProfilePanel.Settings
            else -> ProfilePanel.Overview
        }
        open(destination)
    }

    fun setTab(tab: String) {
        if (tab in setOf("posts", "vibes", "saved")) {
            _state.update { it.copy(activeTab = tab) }
            // A bookmark can change while Profile is retained in the navigation
            // back stack, so entering Saved always reconciles with the server.
            if (tab == "saved") loadSavedPosts(force = true)
        }
    }

    private fun loadSavedPosts(force: Boolean = false) {
        val current = _state.value
        if (current.savedLoading || (current.savedLoaded && !force)) return
        viewModelScope.launch {
            _state.update { it.copy(savedLoading = true, savedError = null) }
            runCatching { repository.loadSavedPosts() }
                .onSuccess { posts ->
                    _state.update {
                        it.copy(
                            savedPosts = posts,
                            savedLoading = false,
                            savedLoaded = true,
                            savedError = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            savedLoading = false,
                            savedLoaded = true,
                            savedError = error.message?.takeIf(String::isNotBlank)
                                ?: "Saved posts could not be loaded."
                        )
                    }
                }
        }
    }

    fun updateDraft(transform: (ProfileEditDraft) -> ProfileEditDraft) {
        _state.update { it.copy(editDraft = transform(it.editDraft), error = null) }
    }

    fun saveProfile() {
        val request = runCatching { _state.value.editDraft.toRequest() }
            .getOrElse {
                _state.update { state -> state.copy(error = it.message) }
                return
            }
        mutate("Profile updated.") {
            repository.updateProfile(request)
            repository.load()
        }
    }

    fun uploadAvatar(resolver: ContentResolver, uri: Uri) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching {
                val avatarUrl = repository.uploadAvatar(resolver, uri)
                val request = _state.value.editDraft.copy(avatarUrl = avatarUrl).toRequest()
                repository.updateProfile(request)
                repository.load()
            }.onSuccess { bundle ->
                val profile = requireNotNull(bundle.privateProfile.profile)
                _state.update {
                    it.copy(
                        busy = false,
                        privateProfile = profile,
                        publicProfile = bundle.publicProfile,
                        editDraft = ProfileEditDraft.from(profile),
                        notice = "Profile photo updated."
                    )
                }
            }.onFailure { fail(it, "Profile photo could not be updated.") }
        }
    }

    fun openConnections(scope: String) {
        val username = _state.value.privateProfile?.username ?: return
        _state.update {
            it.copy(
                panel = ProfilePanel.Connections,
                connectionScope = scope,
                connections = emptyList(),
                busy = true,
                error = null
            )
        }
        viewModelScope.launch {
            runCatching { repository.connections(username, scope) }
                .onSuccess { connections ->
                    _state.update { it.copy(connections = connections, busy = false) }
                }
                .onFailure { fail(it, "Connections could not be loaded.") }
        }
    }

    fun toggleConnectionFollow(connection: ProfileConnection) {
        if (connection.isViewer || _state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching { repository.setFollowing(connection.username, !connection.isFollowing) }
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            busy = false,
                            connections = it.connections.map { item ->
                                if (item.username == response.username) {
                                    item.copy(isFollowing = response.isFollowing)
                                } else item
                            }
                        )
                    }
                }
                .onFailure { fail(it, "Follow status could not be updated.") }
        }
    }

    fun setPrivacy(settings: ChatPrivacySettings) {
        _state.update { it.copy(privacy = settings, error = null) }
    }

    fun savePrivacy() {
        val settings = _state.value.privacy
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { repository.updatePrivacy(settings) }
                .onSuccess { saved ->
                    _state.update {
                        it.copy(
                            privacy = saved,
                            busy = false,
                            notice = "Chat privacy updated."
                        )
                    }
                }
                .onFailure { fail(it, "Privacy settings could not be saved.") }
        }
    }

    fun setContentMeasurementEnabled(enabled: Boolean) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { repository.setContentMeasurementEnabled(enabled) }
                .onSuccess { persisted ->
                    _state.update {
                        it.copy(
                            contentMeasurementEnabled = persisted,
                            busy = false,
                            notice = if (persisted) "Creator measurement enabled." else "Creator measurement paused."
                        )
                    }
                }
                .onFailure { fail(it, "Measurement preference could not be saved.") }
        }
    }

    fun eraseContentMeasurement() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { repository.eraseContentMeasurement() }
                .onSuccess {
                    _state.update {
                        it.copy(busy = false, notice = "Your raw measurement history was erased.")
                    }
                }
                .onFailure { fail(it, "Measurement history could not be erased.") }
        }
    }

    fun revokeDevice(device: TrustedDevice) {
        if (device.isCurrentDevice || _state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { repository.revokeDevice(device.id) }
                .onSuccess { devices ->
                    _state.update {
                        it.copy(
                            devices = devices,
                            busy = false,
                            notice = "${device.label} was revoked."
                        )
                    }
                }
                .onFailure { fail(it, "Trusted device could not be revoked.") }
        }
    }

    fun sendPasswordReset() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { repository.sendPasswordReset() }
                .onSuccess { message -> _state.update { it.copy(busy = false, notice = message) } }
                .onFailure { fail(it, "Password reset email could not be sent.") }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(error = null, notice = null) }
    }

    private fun mutate(notice: String, block: suspend () -> OwnProfileBundle) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            runCatching { block() }
                .onSuccess { bundle ->
                    val profile = requireNotNull(bundle.privateProfile.profile)
                    _state.update {
                        it.copy(
                            busy = false,
                            panel = ProfilePanel.Overview,
                            privateProfile = profile,
                            publicProfile = bundle.publicProfile,
                            privacy = bundle.privacy,
                            devices = bundle.devices,
                            editDraft = ProfileEditDraft.from(profile),
                            notice = notice
                        )
                    }
                }
                .onFailure { fail(it, "Changes could not be saved.") }
        }
    }

    private fun fail(error: Throwable, fallback: String) {
        _state.update {
            it.copy(
                loading = false,
                busy = false,
                error = error.message?.takeIf(String::isNotBlank) ?: fallback
            )
        }
    }
}

package social.vyb.app.features.profile

import kotlinx.serialization.Serializable
import social.vyb.app.data.ProfileEnvelope
import social.vyb.app.features.search.CampusPerson
import social.vyb.app.features.search.PublicProfileResponse
import social.vyb.app.features.social.SocialPost

@Serializable
internal data class ProfileUploadedAsset(val url: String)

@Serializable
internal data class ProfileMediaUploadEnvelope(val asset: ProfileUploadedAsset)

@Serializable
internal data class ProfileSavedPostsEnvelope(
    val items: List<SocialPost> = emptyList()
)

@Serializable
data class ProfileConnection(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val collegeName: String = "",
    val course: String = "",
    val stream: String = "",
    val bio: String? = null,
    val isFollowing: Boolean = false,
    val isViewer: Boolean = false
) {
    fun asCampusPerson() = CampusPerson(
        userId = userId,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        collegeName = collegeName,
        course = course,
        stream = stream,
        bio = bio,
        isFollowing = isFollowing
    )
}

@Serializable
internal data class ProfileConnectionsResponse(
    val profileUsername: String,
    val scope: String,
    val items: List<ProfileConnection> = emptyList()
)

@Serializable
data class ChatPrivacySettings(
    val lastSeenOnline: String = "My Contacts",
    val readReceipts: Boolean = true,
    val typingIndicator: Boolean = true,
    val updatedAt: String = ""
)

@Serializable
internal data class ChatPrivacyEnvelope(
    val settings: ChatPrivacySettings
)

@Serializable
internal data class UpdateChatPrivacyRequest(
    val lastSeenOnline: String,
    val readReceipts: Boolean,
    val typingIndicator: Boolean
)

@Serializable
data class TrustedDevice(
    val id: String,
    val userId: String,
    val membershipId: String,
    val label: String,
    val platform: String = "unknown",
    val publicKey: String,
    val addedAt: String,
    val lastSeenAt: String,
    val revokedAt: String? = null,
    val isCurrentDevice: Boolean = false
)

@Serializable
internal data class TrustedDevicesEnvelope(
    val items: List<TrustedDevice> = emptyList()
)

@Serializable
internal data class RevokeTrustedDeviceResponse(
    val deviceId: String,
    val items: List<TrustedDevice> = emptyList()
)

data class OwnProfileBundle(
    val privateProfile: ProfileEnvelope,
    val publicProfile: PublicProfileResponse,
    val privacy: ChatPrivacySettings,
    val devices: List<TrustedDevice>
)

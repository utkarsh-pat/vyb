package social.vyb.app.features.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialVisualPrimitivesTest {
    @Test
    fun missingOrBlankAvatarUsesNeutralAssetInsteadOfInitials() {
        assertEquals(
            SocialAvatarPresentation.BlankAsset,
            socialAvatarPresentation(null)
        )
        assertEquals(
            SocialAvatarPresentation.BlankAsset,
            socialAvatarPresentation("   ")
        )
    }

    @Test
    fun nonBlankAvatarUsesRemoteImage() {
        assertEquals(
            SocialAvatarPresentation.RemoteImage,
            socialAvatarPresentation("https://cdn.example/avatar.jpg")
        )
    }
}

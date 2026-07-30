package social.vyb.app.features.scribble

import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

internal class ScribbleRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val api: ScribbleApi = VybNetwork.create()
    private val clientId = UUID.randomUUID().toString()

    suspend fun socketUrl(): String {
        val token = api.socketToken(auth.requireBearerToken())
        val separator = if (token.wsUrl.contains("?")) "&" else "?"
        return "${token.wsUrl}${separator}clientId=$clientId"
    }

    suspend fun loadPublicRooms(): List<ScribbleCatalogRoom> {
        val bearer = auth.requireBearerToken()
        val token = api.socketToken(bearer)
        return api.publicRooms(bearer, token.tenantId).rooms
    }
}

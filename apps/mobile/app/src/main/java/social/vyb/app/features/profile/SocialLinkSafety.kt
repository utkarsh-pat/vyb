package social.vyb.app.features.profile

import java.net.URI

internal fun safeSocialUrl(network: String, rawValue: String?): String? {
    val value = rawValue?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val candidate = if ("://" in value) value else "https://$value"
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        uri.userInfo != null ||
        uri.port != -1 ||
        !uri.fragment.isNullOrEmpty()
    ) return null

    val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
    val allowedRoot = when (network.lowercase()) {
        "linkedin" -> "linkedin.com"
        "github" -> "github.com"
        "instagram" -> "instagram.com"
        else -> return null
    }
    if (host != allowedRoot && !host.endsWith(".$allowedRoot")) return null
    return URI(
        "https",
        null,
        host,
        -1,
        uri.rawPath.orEmpty().ifEmpty { "/" },
        uri.rawQuery,
        null
    ).toASCIIString()
}

package social.vyb.app.features.profile

import java.net.URI

internal fun safeSocialUrl(network: String, rawValue: String?): String? {
    val value = rawValue?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val normalizedNetwork = network.lowercase()
    if (normalizedNetwork == "email") {
        val address = value.removePrefix("mailto:").trim()
        if ('\r' in address || '\n' in address ||
            !address.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
        ) return null
        return "mailto:$address"
    }

    val allowedRoots = when (normalizedNetwork) {
        "linkedin" -> setOf("linkedin.com")
        "github" -> setOf("github.com")
        "instagram" -> setOf("instagram.com")
        "twitter", "x" -> setOf("x.com", "twitter.com")
        "codeforces" -> setOf("codeforces.com")
        "leetcode" -> setOf("leetcode.com")
        else -> return null
    }
    if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(value) &&
        !value.startsWith("https://", ignoreCase = true)
    ) return null
    val unprefixed = value.removePrefix("@").trim('/')
    val looksLikeHost = allowedRoots.any { root ->
        unprefixed == root || unprefixed.startsWith("$root/") ||
            unprefixed == "www.$root" || unprefixed.startsWith("www.$root/")
    }
    val handlePath = when (normalizedNetwork) {
        "linkedin" -> "linkedin.com/in/$unprefixed"
        "codeforces" -> "codeforces.com/profile/$unprefixed"
        "leetcode" -> "leetcode.com/u/$unprefixed"
        "twitter", "x" -> "x.com/$unprefixed"
        else -> "${allowedRoots.first()}/$unprefixed"
    }
    if ("://" !in value && !looksLikeHost &&
        !unprefixed.matches(Regex("^[A-Za-z0-9._-]+$"))
    ) return null
    val candidate = when {
        "://" in value -> value
        looksLikeHost -> "https://$unprefixed"
        else -> "https://$handlePath"
    }
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        uri.userInfo != null ||
        uri.port != -1 ||
        !uri.fragment.isNullOrEmpty()
    ) return null

    val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
    if (allowedRoots.none { root -> host == root || host.endsWith(".$root") }) return null
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

package app.localbrief.data

import java.net.URI
import java.util.concurrent.TimeUnit

object Rules {
    const val RETENTION_DAYS = 14L
    fun cutoff(now: Long) = now - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
    fun matches(title: String, preview: String, include: String, exclude: String): Boolean {
        val text = "$title $preview".lowercase()
        fun words(value: String) = value.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val wanted = words(include)
        return (wanted.isEmpty() || wanted.any { it in text }) && words(exclude).none { it in text }
    }
    fun normalizeUrl(value: String): String {
        val uri = URI(value.trim())
        require(uri.scheme?.lowercase() == "https") { "Use an HTTPS link." }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null) { "Invalid link." }
        val host = uri.host.lowercase()
        val youtubeId = when {
            host == "youtu.be" -> uri.path.trim('/').substringBefore('/')
            host in setOf("youtube.com", "www.youtube.com", "m.youtube.com") && uri.path == "/watch" ->
                uri.rawQuery?.split('&')?.firstOrNull { it.substringBefore('=') == "v" }?.substringAfter('=')
            host in setOf("youtube.com", "www.youtube.com", "m.youtube.com") && uri.path.startsWith("/shorts/") -> uri.path.removePrefix("/shorts/").substringBefore('/')
            else -> null
        }
        if (youtubeId != null && Regex("[A-Za-z0-9_-]{1,100}").matches(youtubeId)) return "https://www.youtube.com/watch?v=$youtubeId"
        val query = uri.rawQuery?.split('&')?.filterNot {
            val key = it.substringBefore('=').lowercase()
            key.startsWith("utm_") || key in setOf("fbclid", "gclid")
        }?.joinToString("&")?.takeIf { it.isNotBlank() }
        val authority = host + if (uri.port != -1 && uri.port != 443) ":${uri.port}" else ""
        return "https://$authority${uri.rawPath.orEmpty().ifEmpty { "/" }}" + (query?.let { "?$it" } ?: "")
    }
    fun sharedLink(text: String): String? = Regex("https://[^\\s<>\\\"]+", RegexOption.IGNORE_CASE)
        .find(text)?.value?.trimEnd('.', ',', ')', ']')?.let { runCatching { normalizeUrl(it) }.getOrNull() }
}

package app.localbrief.ui

import java.net.URI

object BrowserPolicy {
    // Validate navigation without rewriting query strings or fragment anchors.
    fun canLoad(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
}

package com.d4rk.cleaner.app.clean.link.domain

import android.net.Uri

object UrlCleaner {
    private val trackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "igshid", "mc_cid", "mc_eid"
    )

    fun clean(url: String): String {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return url }
        if (uri.scheme == null) return url
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames
            .filter { it !in trackingParams }
            .forEach { param ->
                builder.appendQueryParameter(param, uri.getQueryParameter(param))
            }
        return builder.build().toString()
    }
}

package blackorbs.dev.jetfiledownloader.helpers

import android.util.Patterns

object LinkHelper {
    const val INIT_URL = "https://www.kefblog.com.ng"

    fun getValidUrl(text: String) = when{
        (Patterns.WEB_URL.matcher(text).matches()) ->
            text.replaceFirst("http:", "https:")
        else -> "https://www.google.com/search?q=$text"
    }
}
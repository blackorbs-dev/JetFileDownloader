package blackorbs.dev.jetfiledownloader.helpers

object LinkHelper {
    const val INIT_URL = "https://www.kefblog.com.ng"

    fun getAsHttps(url: String) = url.replaceFirst("http:", "https:")
}
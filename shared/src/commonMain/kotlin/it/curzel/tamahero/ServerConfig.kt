package it.curzel.tamahero

object ServerConfig {
    private var _baseUrl: String = "https://tama.curzel.it"

    val BASE_URL: String get() = _baseUrl

    fun overrideBaseUrl(url: String) {
        _baseUrl = url
    }
}

package it.curzel.tamahero.auth

object OAuthConfig {
    val googleClientIdWeb: String get() = System.getenv("GOOGLE_CLIENT_ID_WEB") ?: ""
    val googleClientIdAndroid: String get() = System.getenv("GOOGLE_CLIENT_ID_ANDROID") ?: ""
    val googleClientIdIos: String get() = System.getenv("GOOGLE_CLIENT_ID_IOS") ?: ""
    val googleClientIdDesktop: String get() = System.getenv("GOOGLE_CLIENT_ID_DESKTOP") ?: ""
    val googleClientSecret: String get() = System.getenv("GOOGLE_CLIENT_SECRET") ?: ""
    val googleClientSecretDesktop: String get() = System.getenv("GOOGLE_CLIENT_SECRET_DESKTOP") ?: ""
    val appleBundleId: String get() = System.getenv("APPLE_BUNDLE_ID") ?: ""
    val appleServiceId: String get() = System.getenv("APPLE_SERVICE_ID") ?: ""

    val allGoogleClientIds: List<String>
        get() = listOfNotNull(
            googleClientIdWeb.ifEmpty { null },
            googleClientIdAndroid.ifEmpty { null },
            googleClientIdIos.ifEmpty { null },
            googleClientIdDesktop.ifEmpty { null },
        )

    val allAppleAudiences: List<String>
        get() = listOfNotNull(
            appleBundleId.ifEmpty { null },
            appleServiceId.ifEmpty { null },
        )
}

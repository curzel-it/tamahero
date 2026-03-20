plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "it.curzel.tamahero"
version = "1.0.0"
application {
    mainClass.set("it.curzel.tamahero.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationJson)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientContentNegotiation)
    testImplementation(libs.kotlin.testJunit)
}
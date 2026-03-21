plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "it.curzel.tamahero"
version = "1.0.0"

application {
    mainClass.set("it.curzel.tamahero.cli.MainKt")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinx.serialization.json)
    implementation("io.ktor:ktor-client-cio-jvm:3.3.3")
    implementation("io.ktor:ktor-client-websockets-jvm:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
    implementation(libs.kotlinx.coroutinesCore)
    testRuntimeOnly(projects.server)
    testImplementation(projects.server)
    testImplementation(libs.ktor.serverCore)
    testImplementation(libs.ktor.serverNetty)
    testImplementation(libs.kotlin.testJunit)
}

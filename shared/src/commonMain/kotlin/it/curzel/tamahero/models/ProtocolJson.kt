package it.curzel.tamahero.models

import kotlinx.serialization.json.Json

val ProtocolJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

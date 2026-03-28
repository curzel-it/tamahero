package it.curzel.tamahero

class WasmPlatform : Platform {
    override val name: String = "Web (WASM)"
}

actual fun getPlatform(): Platform = WasmPlatform()

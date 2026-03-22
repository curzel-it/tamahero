# Hello Claude!

## Handling a Task
1. If the task is not too simple, use Claude Code's built-in plan mode to create a comprehensive plan of what needs to change, where, with code links and code snippets (especially of data structures)
2. Ask me any questions about things that are uncertain in the plan (as a numbered list)
3. Update the plan with my answers
4. Implement and test frequently
5. Review and cleanup, remove unnecessary comments
6. Enjoy!

## Core Principles

### Keep It Simple Stupid (KISS)
- Write the simplest code that works
- Avoid over-engineering
- Don't add features that aren't requested
- Prefer clarity over cleverness
- Remove unnecessary abstractions

### One Feature = One File
When adding a new feature, create a new file for that feature.
Ideally, new features are "wired up" with just a few lines of code for configuration and setup.

### Clean Architecture
Have dedicated files for `SomethingView`, `SomethingViewModel` and `SomethingUseCase` where applicable.

**Example:** For a connection status indicator:
- `ConnectionStatusView.kt`
- `ConnectionStatusViewModel.kt`
- `ConnectionStatusUseCase.kt`

## Platform-Specific Code

**NEVER use actual/expect.**

Use simple dependency injection with static providers:

### Pattern Example

In `composeApp/commonMain/SomeDependency.kt`:
```kotlin
interface SomeDependency {
    fun doSomething()
}

object SomeDependencyProvider {
    lateinit var instance: SomeDependency
        private set

    fun setProvider(dependency: SomeDependency) {
        instance = dependency
    }
}
```

In `composeApp/androidMain/SomeDependencyAndroid.kt`:
```kotlin
class SomeDependencyAndroid(val context: Context) : SomeDependency {
    override fun doSomething() {
        // Android-specific implementation
    }
}
```

In `composeApp/iosMain/SomeDependencyIos.kt`:
```kotlin
class SomeDependencyIos : SomeDependency {
    override fun doSomething() {
        // iOS-specific implementation
    }
}
```

In `composeApp/jvmMain/SomeDependencyDesktop.kt`:
```kotlin
class SomeDependencyDesktop : SomeDependency {
    override fun doSomething() {
        // Desktop-specific implementation
    }
}
```

Wire up in platform entry points (MainActivity, iOSApp, main.kt).

## Building & Testing

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Android
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

Or use Android Studio:
- Open the project root directory
- Run on connected Android device or emulator

### iOS
```bash
./gradlew :composeApp:iosSimulatorArm64Test
```

Or open `iosApp/iosApp.xcodeproj` in Xcode and run from there.

### Server
```bash
./gradlew :server:run
```

### CLI Client
```bash
./gradlew :cli:run --console=plain
```

Or with a custom server URL:
```bash
./gradlew :cli:run --console=plain --args="https://tama.curzel.it"
```

### Testing
```bash
./gradlew :shared:jvmTest :server:test :cli:test
```
- All code should be unit-testable
- Test frequently to catch compilation errors
- Build after every significant change

## Project Structure

```
tamahero/
├── shared/            # Models, use cases, protocol (used by all modules)
├── server/            # Ktor server (WebSocket + REST auth)
├── cli/               # CLI client (for testing & development)
├── composeApp/        # Compose Multiplatform UI
│   ├── commonMain     # Shared game rendering
│   ├── androidMain    # Android implementation
│   ├── iosMain        # iOS implementation
│   └── jvmMain        # Desktop implementation
└── iosApp/            # iOS app wrapper
```

## Key Technologies

- Kotlin Multiplatform, Compose Multiplatform
- Targets: Desktop (JVM), Android, iOS

## Style Guidelines

- Stick to style and preferences existing in the current codebase
- After implementing changes, run a code review on all edited files
- Before marking tasks as completed, remove all unnecessary comments
- Implement the cleanest and simplest possible version of features

## Development Workflow

1. **Make changes**: Edit code in appropriate source set (commonMain, androidMain, iosMain, jvmMain)
2. **Build**: Run gradle build to verify compilation
3. **Test**: Run on target platform (desktop is fastest for iteration)
4. **Review**: Clean up and remove unnecessary code/comments
5. **Commit**: Commit frequently with clear messages

## CLI Client Reference

The CLI is the primary tool for testing game features end-to-end. It connects to the server via WebSocket and supports all game actions. Token is saved to `~/.tamahero/token.json` — subsequent runs auto-connect.

### First-time setup
```
> register myuser mypass123
> connect
> village
```

### Subsequent runs (auto-connects with saved token)
```
> village                          # Load/refresh village state
```

### Commands

**Connection:**
- `register <user> <pass>` — create account (saves token)
- `login <user> <pass>` — login (saves token)
- `logout` — clear saved token
- `connect` — manually connect WebSocket

**Village management:**
- `village` / `get` — load full village state
- `build <Type> <x> <y>` — place building (e.g. `build LumberCamp 5 5`)
- `upgrade <id>` — upgrade building by ID
- `move <id> <x> <y>` — relocate building
- `demolish <id>` — remove building (50% refund)
- `cancel <id>` — cancel construction (full refund)
- `speedup <id>` — instant finish with mana

**Resources:**
- `collect <id>` — collect from a producer
- `collectall` — collect from all producers
- `storage` — show current vs max capacity

**Troops:**
- `train <Type> [count]` — queue training (e.g. `train HumanSoldier 5`)
- `canceltraining <index>` — cancel queued entry
- `army` — show army + training queue
- `troops` — list troop types with stats

**Defense:**
- `rearm <id>` — rearm triggered trap (50% cost)
- `rearmall` — rearm all traps

**Events:**
- `event` — show active PvE event status
- `collectrewards` — claim completed event rewards

**Info:**
- `info <id>` — building details + upgrade cost
- `buildings` — list all building types
- `map` — ASCII village map
- `help` — full command list

### Building types
TownHall, LumberCamp, GoldMine, Forge, WoodStorage, GoldStorage, MetalStorage,
Barracks, ArmyCamp, Cannon, ArcherTower, Mortar, Wall, SpikeTrap, SpringTrap, ShieldDome

### Troop types
HumanSoldier, ElfArcher, DwarfSapper, OrcBerserker

## Rendering

Rendering uses Compose Canvas with a camera/zoom system:

In `RenderingSprites.kt`, define draw functions:
```kotlin
fun DrawScope.drawSomething(entity: SomethingEntity, camera: Vector2d, zoom: Float) {
    // Draw sprites relative to camera position
}
```

In `RenderingView.kt`, compose the full scene by iterating over game objects and calling sprite draw functions, sorted by z-index for correct layering.

## Remember

- Keep it simple
- One feature = one file
- No actual/expect, use DI
- Test frequently
- Build on all platforms before considering task complete

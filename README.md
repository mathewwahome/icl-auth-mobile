# OHS Player Reference Client App

A Kotlin Multiplatform and Compose Multiplatform reference client for [Open Health Stack (OHS)](https://developers.google.com/open-health-stack/overview). A single Kotlin source tree targets Android, iOS, JVM desktop, JS browser, and Wasm browser.

The application renders healthcare UI from configuration rather than hand-written mapping code. FHIR resources are projected into typed view-state by declarative configuration, and that state is rendered by renderers resolved through a registry. The two halves — extraction and rendering — are described below, then joined in a single end-to-end example.

## Getting started

### Prerequisites

- JDK 21
- Android SDK (for Android builds)
- Xcode (for iOS builds, macOS only)

Use `./gradlew` on macOS and Linux, and `gradlew.bat` on Windows. All commands run from the repository root.

### Build

```shell
git clone <repository-url>
cd ohs-player-reference-client-app
./gradlew build
```

Code generation is part of compilation. The `ig-codegen` Gradle plugin runs its `generateIgCode` task automatically before Kotlin compilation, so there is no separate generation step.

### Run

| Target | Command |
| --- | --- |
| Android | `./gradlew :ohs-player-reference-app:assembleDebug` |
| Desktop (JVM) | `./gradlew :ohs-player-reference-app:run` |
| Web (Wasm) | `./gradlew :ohs-player-reference-app:wasmJsBrowserDevelopmentRun` |
| Web (JS) | `./gradlew :ohs-player-reference-app:jsBrowserDevelopmentRun` |

For iOS, open [`iosApp/`](./iosApp) in Xcode and run, or use the run-configuration widget in a Kotlin Multiplatform IDE.

## From FHIR data to view state

A screen never consumes a raw FHIR resource. It consumes a typed *view-state* — a flat, serializable data class containing exactly the fields the screen needs. View-state is produced by a configuration-driven pipeline:

1. **Author** configuration as FHIR `Binary` resources (a `ViewDefinition`, a `ViewJoinMap`, and a `ViewConfig`).
2. **Generate** typed Kotlin from those Binaries at build time via the `ig-codegen` plugin.
3. **Load** the Binaries at runtime through a `ConfigStore`.
4. **Extract** view-state from a `SearchResult` with `GenericStateExtractor.extract<T>()`.

### 1. Author configuration

A `ViewDefinition` declares the columns of a view as FHIRPath expressions over a FHIR resource. Each column carries a name, a path, and a FHIR type. Excerpt from [`Binary-PatientSummary.json`](./ohs-player-reference-app/src/commonMain/composeResources/files/states/Binary-PatientSummary.json):

```json
{
  "resourceType": "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition",
  "name": "PatientSummary",
  "status": "active",
  "resource": "Patient",
  "select": [
    {
      "column": [
        { "name": "patientId", "path": "id", "type": "http://hl7.org/fhir/StructureDefinition/string" },
        { "name": "familyName", "path": "name.family.first()", "type": "http://hl7.org/fhir/StructureDefinition/string" },
        { "name": "gender", "path": "gender", "type": "http://hl7.org/fhir/StructureDefinition/code" },
        { "name": "active", "path": "active", "type": "http://hl7.org/fhir/StructureDefinition/boolean" }
      ]
    }
  ]
}
```

A `ViewJoinMap` names the view-state and binds it to a pivot `ViewDefinition` (and, where needed, joined views). [`Binary-PatientSummaryState.json`](./ohs-player-reference-app/src/commonMain/composeResources/files/states/Binary-PatientSummaryState.json):

```json
{
  "resourceType": "http://ohs.dev/StructureDefinition/ViewJoinMap",
  "name": "patientSummary",
  "from": "root",
  "resource": "Patient",
  "view": "PatientSummary"
}
```

A `ViewConfig` declares the configuration a renderer accepts, with defaults. [`Binary-PatientCardConfig.json`](./ohs-player-reference-app/src/commonMain/composeResources/files/configs/Binary-PatientCardConfig.json):

```json
{
  "resourceType": "http://ohs.dev/StructureDefinition/ViewConfig",
  "viewType": "PatientCard",
  "property": [
    { "name": "showStatusChip", "type": "boolean", "valueBoolean": true },
    { "name": "showAge", "type": "boolean", "valueBoolean": true },
    { "name": "elevation", "type": "decimal", "valueDecimal": 2.0 }
  ]
}
```

A single `CodeSystem` Binary enumerates the view-types the app renders; see [`CodeSystem-ViewTypes.json`](./ohs-player-reference-app/src/commonMain/composeResources/files/viewtypes/CodeSystem-ViewTypes.json).

### 2. Generate typed Kotlin

The `ig-codegen` plugin reads these Binaries and emits typed sources. It is applied and configured in [`ohs-player-reference-app/build.gradle.kts`](./ohs-player-reference-app/build.gradle.kts):

```kotlin
plugins {
    id("dev.ohs.ig-codegen")
}

igCodegen {
    // sourcesDir defaults to src/commonMain/composeResources/files
    packageName = "dev.ohs.player.generated"
}
```

Inputs live under `src/commonMain/composeResources/files/`, organised as `states/` (ViewDefinition and ViewJoinMap), `configs/` (ViewConfig), and `viewtypes/` (the CodeSystem). The generated symbols are:

| Generated symbol | Source | Package |
| --- | --- | --- |
| `PatientSummaryState` and other `*State` classes | ViewJoinMap + columns | `dev.ohs.player.generated.state` |
| `PatientCardConfig` and other `*Config` classes | ViewConfig | `dev.ohs.player.generated.config` |
| `ViewTypeCS` | CodeSystem | `dev.ohs.player.generated.viewtype` |
| `GeneratedConfigManifest` | file listing | `dev.ohs.player.generated` |

`PatientSummaryState`, for example, is generated as:

```kotlin
@Serializable
data class PatientSummaryState(
    val patientId: String? = null,
    val familyName: String? = null,
    val givenName: String? = null,
    val gender: String? = null,
    val birthDate: FhirDate? = null,
    val active: Boolean? = null,
    val mrn: String? = null,
    val phone: String? = null,
)
```

### 3. Load configuration at runtime

A `ConfigStore` holds the parsed configuration, fed by a `ConfigSource`. The reference app reads the bundled Binaries; replacing this with a network fetch is the only change required to load configuration from a backend. See [`LocalConfigSource.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/datasource/LocalConfigSource.kt):

```kotlin
object LocalConfigSource : ConfigSource {
    private const val DIR_NAME = "states"

    override suspend fun readAll(): List<String> =
        GeneratedConfigManifest.byDirectory[DIR_NAME].orEmpty().map { fileName ->
            Res.readBytes("files/$DIR_NAME/$fileName").decodeToString()
        }
}
```

The store and a single extractor are wired once in [`Extraction.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/Extraction.kt):

```kotlin
object Extraction {
    private val configStore: ConfigStore = ConfigStore(LocalConfigSource)
    val extractor: GenericStateExtractor = GenericStateExtractor(configStore)
}
```

### 4. Extract view-state

`GenericStateExtractor.extract<T>()` selects the configuration for `T` by name, evaluates its FHIRPath columns against a `SearchResult`, and returns a list of typed `T`. A `SearchResult` carries the pivot resource plus any forward-included and reverse-included resources, mirroring a FHIR search response.

From [`PatientRepository.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/repository/PatientRepository.kt):

```kotlin
suspend fun getPatients(): List<PatientSummaryState> =
    withContext(extractorDispatcher) {
        allPatientIds().mapNotNull { id ->
            patientSummarySearchResult(id)?.let {
                extractor.extract<PatientSummaryState>(it).firstOrNull()
            }
        }
    }
```

The FHIRPath engine holds mutable evaluation state and is not safe for concurrent use. Serialize extraction onto a single thread; the repository does this with `Dispatchers.Default.limitedParallelism(1)`.

## Rendering view state

View-state is rendered by renderers resolved through a registry, so screens depend on view-types rather than concrete UI classes:

1. **Author** a `ComponentRenderer` for a view-state type.
2. **Register** it under a generated `ViewTypeCS` view-type in a `ViewRegistry`.
3. **Install** the registry into the composition via `LocalViewRegistry`.
4. **Render** with `ListScaffold` or `DetailScaffold`, which resolve renderers by view-type.

### 1. Author a renderer

A `ComponentRenderer<T, C>` renders one item of state `T` with configuration `C`. One renderer class can be registered under several view-types with different configurations.

```kotlin
class PatientCardRenderer : ComponentRenderer<PatientSummaryState, PatientCardConfig> {
    @Composable
    override fun Render(
        item: PatientSummaryState,
        config: PatientCardConfig,
        options: RenderOptions,
    ) {
        PatientCard(patient = item, config = config, onClick = options.onClick, modifier = options.modifier)
    }
}
```

`RenderOptions` carries the optional tap handler and root modifier. `LayoutRenderer<T>` is the corresponding arrangement abstraction; the library ships `VerticalListRenderer`, `HorizontalListRenderer`, and `GridListRenderer`.

### 2. Register renderers

Group a feature's registrations into an extension on `ViewRegistry`. See [`PatientListRegistrations.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListRegistrations.kt):

```kotlin
fun ViewRegistry.registerPatientList() {
    registerComponent<PatientSummaryState, PatientCardConfig>(
        ViewTypeCS.PatientCard,
        PatientCardRenderer(),
        PatientCardConfig(),
    )
    registerLayout<PatientSummaryState>(
        VerticalListRenderer.VIEW_TYPE,
        VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
}
```

Assemble all feature registrations in one builder, as in [`AppViewRegistry.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewRegistry.kt):

```kotlin
fun buildAppViewRegistry(): ViewRegistry = ViewRegistry().apply {
    registerPatientList()
    registerPatientProfile()
}
```

A registry lookup is keyed by both view-type and state type, and throws `NoSuchElementException` naming the missing key if a renderer was not registered.

### 3. Install the registry

Provide the registry at the composition root so every screen can resolve renderers. See [`App.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/App.kt):

```kotlin
@Composable
fun App() {
    val registry = remember { buildAppViewRegistry() }
    CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
            // NavHost, screens, etc.
        }
    }
}
```

### 4. Render

`ListScaffold` renders a list; `component(...)` and `layout(...)` name the view-types to resolve. An empty list short-circuits to `emptyState` without invoking the layout renderer, and omitting `layout(...)` falls back to `VerticalListRenderer`. See [`PatientListScreen.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListScreen.kt):

```kotlin
ListScaffold<PatientSummaryState>(
    items = patients,
    onItemClick = { onPatientClick(it.patientId ?: "") },
    key = { it.patientId ?: it.hashCode().toString() },
) {
    component(ViewTypeCS.PatientCard)
    layout(VerticalListRenderer.VIEW_TYPE)
    topBar { TopAppBar(title = { Text("Patients") }) }
    emptyState { Text("No patients") }
}
```

`DetailScaffold` is the single-item counterpart: it renders a stack of sections for one nullable item, falling back to a `notFound` slot when the item is absent.

## End-to-end example

A patient list screen exercises both halves of the pipeline:

1. **Configuration.** `Binary-PatientSummary.json` declares the columns; `Binary-PatientSummaryState.json` names the `patientSummary` view-state. `ig-codegen` generates `PatientSummaryState`.
2. **Extraction.** `PatientRepository.getPatients()` builds a `SearchResult` per patient and calls `extractor.extract<PatientSummaryState>(result)`, yielding `List<PatientSummaryState>`.
3. **Registration.** `registerPatientList()` binds `PatientCardRenderer` to `ViewTypeCS.PatientCard` for `PatientSummaryState`, and `buildAppViewRegistry()` installs it at the composition root.
4. **Rendering.** `PatientListScreen` passes the extracted states to `ListScaffold`, which resolves `PatientCard` by view-type and renders each row.

Adding a field is a configuration change: add a column to the `ViewDefinition`, then reference the regenerated state field in the renderer. No extraction or wiring code changes.

## Testing

Run all multiplatform tests:

```shell
./gradlew :ohs-player-library:allTests :ohs-player-reference-app:allTests
```

Run JVM tests only:

```shell
./gradlew :ohs-player-library:jvmTest :ohs-player-reference-app:jvmTest
```

## Deployment

### Release pipeline

Releases are produced by the [`release.yml`](./.github/workflows/release.yml) GitHub Actions workflow, triggered by a semantic version tag (`vX.Y.Z` or `vX.Y.Z-suffix`). The workflow builds and signs every platform, then publishes a GitHub Release with checksummed artifacts:

- Android APK (`assembleRelease`)
- Desktop installers: Linux `.deb` and `.rpm`, Windows `.msi`, macOS `.dmg`
- A portable Linux tarball (`createDistributable`)

A `workflow_dispatch` run performs a dry run: it builds, signs, and uploads artifacts without publishing a Release. The web (Wasm) and GitHub Pages jobs are currently gated off (`if: false`) pending a larger build runner; the web preview is deployed manually in the interim.

### Local installers

Build a native installer or distributable locally:

```shell
./gradlew :ohs-player-reference-app:packageDmg                 # macOS .dmg
./gradlew :ohs-player-reference-app:packageMsi                 # Windows .msi
./gradlew :ohs-player-reference-app:packageDeb                 # Linux .deb
./gradlew :ohs-player-reference-app:createDistributable        # portable app image
./gradlew :ohs-player-reference-app:wasmJsBrowserDistribution  # web bundle
```

### Android release signing

Release builds read signing inputs from environment variables first, then from a `keystore.properties` file as a development fallback. To produce a signed release locally:

```shell
cp keystore.properties.template keystore.properties
# Edit keystore.properties with your keystore path, alias, and passwords, then:
./gradlew :ohs-player-reference-app:bundleRelease
```

`keystore.properties` is gitignored and must never be committed. The environment variables `ANDROID_KEYSTORE_PATH`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and `ANDROID_STORE_PASSWORD` take precedence over the file when both are set. If neither is configured, release builds are emitted unsigned.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform), and [Kotlin/Wasm](https://kotl.in/wasm/).

/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)
  alias(libs.plugins.kotlinSerialization)
  id("dev.ohs.ig-codegen")
  id("spotless-conventions")
}

kotlin {
  androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "OhsPlayerReferenceApp"
      isStatic = true
    }
  }

  jvm()

  js {
    browser()
    binaries.executable()
  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    binaries.executable()
  }

  sourceSets {
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.activity.compose)
    }
    commonMain.dependencies {
      implementation(project(":icl-auth"))
      implementation(project(":ohs-player-library"))
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material)
      implementation(libs.compose.material3)
      implementation(libs.compose.materialIconsCore)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.kermit)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.datetime)
      implementation(libs.navigation.compose)
      implementation(libs.ohs.fhir.model)
      implementation(libs.ohs.fhir.path)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.compose.uiTest)
      implementation(libs.kotlinx.coroutines.test)
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
    }
    jvmTest.dependencies { implementation(compose.desktop.currentOs) }
  }
}

// Release signing inputs: env vars first (CI), then keystore.properties as a
// dev-time fallback. Read via the providers API so the config cache tracks them.
val keystoreProperties: Map<String, String> =
  providers
    .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
    .asText
    .map { text ->
      val props = Properties().apply { load(text.reader()) }
      props.stringPropertyNames().associateWith(props::getProperty)
    }
    .getOrElse(emptyMap())

/*
 * Reads an environment variable, but yields a value only when it is non-blank.
 * Blank or whitespace-only vars are treated as absent, leaving the provider
 * empty so downstream `.getOrElse(...)` / `.orNull` fallbacks take over. This
 * stops an accidentally exported-but-empty var (e.g. `VERSION_NAME=`) from
 * slipping past those fallbacks.
 */
fun nonBlankEnv(name: String): Provider<String> =
  providers.environmentVariable(name).filter { it.isNotBlank() }

fun envOrKeystore(envName: String, fileKey: String): String? =
  nonBlankEnv(envName).orNull ?: keystoreProperties[fileKey]?.takeIf { it.isNotBlank() }

// "0.0.0-dev" flags accidental dev builds and prevents silent "1.0" CI fallbacks if the version is
// unspecified

val releaseVersionName: String =
  nonBlankEnv("VERSION_NAME").map { it.removePrefix("v") }.getOrElse("0.0.0-dev")

val releaseVersionCode: Int =
  nonBlankEnv("VERSION_CODE")
    .map { raw -> raw.toIntOrNull() ?: error("VERSION_CODE='$raw' must be an integer") }
    .getOrElse(1)

val keystorePath = envOrKeystore("ANDROID_KEYSTORE_PATH", "KEYSTORE_PATH")
val keystoreAlias = envOrKeystore("ANDROID_KEY_ALIAS", "KEY_ALIAS")
val keystoreKeyPassword = envOrKeystore("ANDROID_KEY_PASSWORD", "KEY_PASSWORD")
val keystoreStorePassword = envOrKeystore("ANDROID_STORE_PASSWORD", "STORE_PASSWORD")

val hasReleaseSigning: Boolean =
  !keystorePath.isNullOrBlank() &&
    !keystoreAlias.isNullOrBlank() &&
    !keystoreKeyPassword.isNullOrBlank() &&
    !keystoreStorePassword.isNullOrBlank()

android {
  namespace = "dev.ohs.player.reference.app"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "dev.ohs.player.reference.app"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = releaseVersionCode
    versionName = releaseVersionName
  }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        // Disables legacy V1 JAR signing to rely entirely on V2+ signatures required by Android
        // 7.0+.

        enableV1Signing = false
        enableV2Signing = true
        storeFile = file(keystorePath!!)
        keyAlias = keystoreAlias
        keyPassword = keystoreKeyPassword
        storePassword = keystoreStorePassword
      }
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

igCodegen {
  // sourcesDir defaults to src/commonMain/composeResources/files
  packageName = "dev.ohs.player.generated"
}

dependencies { debugImplementation(libs.compose.uiTooling) }

/*
 * Desktop installer version. WiX/MSI (and jpackage) require a strict numeric
 * MAJOR.MINOR.PATCH, so any Semantic Version pre-release suffix is stripped here. Android's
 * versionName keeps the full string; this drift between platforms is intentional.
 *
 * Example — VERSION_NAME=v1.2.3-alpha.1:
 *   before (raw input)          v1.2.3-alpha.1
 *   Android versionName         1.2.3-alpha.1   (prefix dropped, suffix kept)
 *   Desktop packageVersion      1.2.3           (prefix + suffix stripped)
 *
 * A plain release (VERSION_NAME=v1.2.3) yields 1.2.3 on both platforms.
 */
val composePackageVersion: String =
  nonBlankEnv("VERSION_NAME")
    .map { raw ->
      val numeric = raw.removePrefix("v").substringBefore('-')
      if (numeric.matches(Regex("""\d+\.\d+\.\d+"""))) {
        numeric
      } else {
        error("VERSION_NAME='$raw' is not MAJOR.MINOR.PATCH; cannot derive jpackage packageVersion")
      }
    }
    .getOrElse("1.0.0")

// Targets skipped on CI until their test setups are sorted out. Only disabled when CI=true (set by
// GitHub Actions) so the CI build stays green; local development still runs these so contributors
// can reproduce and fix the underlying failures.
//
//   * Kotlin/JS IR backend crashes lowering the generated sealed-interface dispatch tables in
//     dev.ohs.fhir:fhir-path (StackOverflow in KotlinLikeDumper). The main JS compile is fine; only
//     the JS *test* executable lowering trips because the test source set exercises those types.
//     Mirrors the same skip in :ohs-player-library.
//
//   * Android/JVM host unit tests need a host Android framework (NoClassDefFoundError).
//
// TODO: Adopt Compose Multiplatform UI tests (runComposeUiTest) for commonTest so the host tests
//  run without the Android framework. JVM and iOS execution cover the same logic in the meantime.
val isCi = providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)

if (isCi) {
  tasks
    .matching {
      it.name in
        setOf(
          "testDebugUnitTest",
          "testReleaseUnitTest",
          "compileTestDevelopmentExecutableKotlinJs",
          "compileTestProductionExecutableKotlinJs",
          "jsBrowserTest",
          "wasmJsBrowserTest",
        )
    }
    .configureEach { enabled = false }
}

compose.desktop {
  application {
    mainClass = "dev.ohs.player.reference.app.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
      packageName = "dev.ohs.player.reference.app"
      packageVersion = composePackageVersion
    }
  }
}

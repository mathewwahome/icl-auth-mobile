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
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKotlinMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  id("spotless-conventions")
}

kotlin {
  // TODO(AGP-9.0): rename `androidLibrary { }` to `android { }` once AGP is upgraded.
  androidLibrary {
    namespace = "dev.ohs.player.auth"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest {}
  }

  iosArm64()
  iosSimulatorArm64()

  jvm()

  js { browser() }

  @OptIn(ExperimentalWasmDsl::class) wasmJs { browser() }

  sourceSets {
    all {
      kotlin.setSrcDirs(emptyList<String>())
      resources.setSrcDirs(emptyList<String>())
    }

    commonMain {
      kotlin.setSrcDirs(listOf("src/libraryCommonMain/kotlin"))
      dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.cio)
      }
    }

    commonTest {
      kotlin.setSrcDirs(listOf("src/libraryCommonTest/kotlin"))
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.ktor.client.mock)
      }
    }
  }
}

val isCi = providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)

if (isCi) {
  tasks
    .matching {
      it.name in
        setOf(
          "compileTestDevelopmentExecutableKotlinJs",
          "compileTestProductionExecutableKotlinJs",
          "jsBrowserTest",
          "wasmJsBrowserTest",
          "testAndroidHostTest",
        )
    }
    .configureEach { enabled = false }
}

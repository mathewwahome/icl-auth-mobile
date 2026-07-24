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
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKotlinMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  id("maven-publish")
  id("spotless-conventions")
  id("signing")
}

group = providers.gradleProperty("POM_GROUP_ID").getOrElse("io.github.intellisoft-consulting")

version =
  providers
    .gradleProperty("VERSION_NAME")
    .orElse(providers.environmentVariable("VERSION_NAME"))
    .orElse(providers.environmentVariable("GITHUB_REF_NAME").map { it.removePrefix("v") })
    .getOrElse("0.1.0-SNAPSHOT")

kotlin {
  // TODO(AGP-9.0): rename `androidLibrary { }` to `android { }` once AGP is upgraded.
  androidLibrary {
    namespace = "icl.ohs.libs.auth"
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
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.ui)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.materialIconsCore)
      implementation(libs.compose.materialIconsExtended)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.cio)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.ktor.client.mock)
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

configure<PublishingExtension> {
  repositories {
    mavenLocal()

    maven {
      name = "GitHubPackages"
      url =
        uri(
          providers
            .gradleProperty("MAVEN_REPOSITORY_URL")
            .orElse(providers.environmentVariable("MAVEN_REPOSITORY_URL"))
            .orElse(
              providers.environmentVariable("GITHUB_REPOSITORY").map {
                "https://maven.pkg.github.com/$it"
              }
            )
            .getOrElse("https://maven.pkg.github.com/IntelliSOFT-Consulting/icl-auth-mobile")
        )

      credentials {
        username =
          providers
            .gradleProperty("MAVEN_USERNAME")
            .orElse(providers.environmentVariable("MAVEN_USERNAME"))
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
            .orNull
        password =
          providers
            .gradleProperty("MAVEN_PASSWORD")
            .orElse(providers.environmentVariable("MAVEN_PASSWORD"))
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
            .orNull
      }
    }
  }

  publications.withType<MavenPublication>().configureEach {
    pom {
      name.set(providers.gradleProperty("POM_NAME").getOrElse("ICL Auth"))
      description.set(
        providers
          .gradleProperty("POM_DESCRIPTION")
          .getOrElse("Kotlin Multiplatform auth UI library for Compose Multiplatform apps.")
      )
      url.set(
        providers
          .gradleProperty("POM_URL")
          .getOrElse("https://github.com/IntelliSOFT-Consulting/icl-auth-mobile")
      )

      licenses {
        license {
          name.set(
            providers.gradleProperty("POM_LICENSE_NAME").getOrElse("Apache License, Version 2.0")
          )
          url.set(
            providers
              .gradleProperty("POM_LICENSE_URL")
              .getOrElse("https://www.apache.org/licenses/LICENSE-2.0.txt")
          )
        }
      }

      developers {
        developer {
          id.set(providers.gradleProperty("POM_DEVELOPER_ID").getOrElse("intellisoft-consulting"))
          name.set(
            providers.gradleProperty("POM_DEVELOPER_NAME").getOrElse("IntelliSOFT Consulting")
          )
          organization.set(
            providers
              .gradleProperty("POM_DEVELOPER_ORGANIZATION")
              .getOrElse("IntelliSOFT Consulting")
          )
          organizationUrl.set(
            providers
              .gradleProperty("POM_DEVELOPER_ORGANIZATION_URL")
              .getOrElse("https://github.com/IntelliSOFT-Consulting")
          )
        }
      }

      scm {
        url.set(
          providers
            .gradleProperty("POM_SCM_URL")
            .getOrElse("https://github.com/IntelliSOFT-Consulting/icl-auth-mobile")
        )
        connection.set(
          providers
            .gradleProperty("POM_SCM_CONNECTION")
            .getOrElse("scm:git:git://github.com/IntelliSOFT-Consulting/icl-auth-mobile.git")
        )
        developerConnection.set(
          providers
            .gradleProperty("POM_SCM_DEVELOPER_CONNECTION")
            .getOrElse("scm:git:ssh://git@github.com/IntelliSOFT-Consulting/icl-auth-mobile.git")
        )
      }
    }
  }
}

configure<SigningExtension> {
  val signingKey =
    providers.gradleProperty("SIGNING_KEY").orElse(providers.environmentVariable("SIGNING_KEY"))
  val signingPassword =
    providers
      .gradleProperty("SIGNING_PASSWORD")
      .orElse(providers.environmentVariable("SIGNING_PASSWORD"))

  if (!signingKey.orNull.isNullOrBlank()) {
    useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
    val publishing = extensions.getByType<PublishingExtension>()
    sign(publishing.publications)
  }
}
